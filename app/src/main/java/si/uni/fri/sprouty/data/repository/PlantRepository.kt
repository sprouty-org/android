package si.uni.fri.sprouty.data.repository

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import si.uni.fri.sprouty.data.database.PlantDao
import si.uni.fri.sprouty.data.model.Plant
import si.uni.fri.sprouty.data.model.PlantIdentificationResponse
import si.uni.fri.sprouty.data.network.PlantApiService

class PlantRepository(
    private val plantDao: PlantDao,
    private val plantApiService: PlantApiService
) {
    private val TAG = "PlantRepository"

    // --- 1. LOCAL DATA FLOW ---
    fun getAllPlants(): Flow<List<Plant>> = plantDao.getAllPlants()

    // --- 2. IDENTIFICATION FLOW (The New Logic) ---

    /**
     * Sends image to backend, gets the AI-generated data, and saves to Room.
     */
    suspend fun identifyAndSavePlant(imagePart: MultipartBody.Part, imageUri: String): PlantIdentificationResponse? {
        return try {
            val response = plantApiService.identifyPlant(imagePart)
            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!

                val localPlant = Plant(
                    firebaseId = result.userPlant.id,
                    speciesName = result.userPlant.speciesName ?: "Unknown",
                    customName = result.userPlant.customName,
                    lastWatered = result.userPlant.lastWatered,
                    healthStatus = result.userPlant.healthStatus ?: "Healthy",

                    // IMAGE: Using the local path we just created
                    imageUrl = imageUri,

                    // MASTER DATA: Mapping these so they are cached offline
                    botanicalFact = result.masterPlant.fact,
                    toxicity = result.masterPlant.tox,
                    growthHabit = result.masterPlant.growth,
                    soilType = result.masterPlant.soil,
                    botanicalType = result.masterPlant.type,

                    // REQUIREMENTS
                    targetWateringInterval = result.masterPlant.waterInterval,
                    requiredLightLevel = result.masterPlant.light
                )

                plantDao.insert(localPlant)
                result
            } else {
                Log.e("REPO", "Error: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("REPO", "Failure: ${e.message}")
            null
        }
    }

    // --- 3. REMOTE SYNC (PULL) ---

    suspend fun syncPlantsFromRemote() {
        try {
            coroutineScope {
                // 1. Start both calls simultaneously
                val userPlantsDeferred = async { plantApiService.getRemoteUserPlants() }
                val masterPlantsDeferred = async { plantApiService.getRemoteMasterPlants() }

                // 2. Wait for both to finish
                val remoteUserPlants = userPlantsDeferred.await()
                val remoteMasterData = masterPlantsDeferred.await()

                // 2. Create a Map for quick lookup: Key is Species Name, Value is MasterPlant object
                val masterMap = remoteMasterData.associateBy { it.speciesName }

                // 3. Map UserPlants to local Entities, enriching them with Master data
                val localPlants = remoteUserPlants.map { userRemote ->
                    val masterPlant =
                        masterMap[userRemote.speciesName] // Find matching botanical info

                    Plant(
                        localId = 0,
                        firebaseId = userRemote.id,
                        speciesName = userRemote.speciesName ?: "Unknown",
                        customName = userRemote.customName ?: userRemote.speciesName,
                        imageUrl = userRemote.imageUrl,
                        lastWatered = userRemote.lastWatered,
                        healthStatus = userRemote.healthStatus ?: "Healthy",
                        connectedSensorId = userRemote.connectedSensorId,
                        notificationsEnabled = userRemote.notificationsEnabled,

                        // Enrichment from Master Data (if it exists)
                        botanicalFact = masterPlant?.fact,
                        toxicity = masterPlant?.tox,
                        growthHabit = masterPlant?.growth,
                        soilType = masterPlant?.soil,
                        botanicalType = masterPlant?.type,
                        lifecycle = masterPlant?.life,
                        fruitInfo = masterPlant?.fruit,
                        uses = masterPlant?.uses,
                        maxHeight = masterPlant?.maxHeight,
                        minTemp = masterPlant?.minT,
                        maxTemp = masterPlant?.maxT,

                        // Requirements
                        targetWateringInterval = masterPlant?.waterInterval ?: 7,
                        requiredLightLevel = masterPlant?.light ?: "Unknown"
                    )
                }

                // 4. Update Database
                plantDao.deleteAll()
                plantDao.insertAll(localPlants)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync: ${e.message}")
        }
    }

    // --- 4. DELETE ---

    suspend fun deletePlant(plant: Plant) {
        plant.firebaseId?.let { id ->
            try {
                plantApiService.deletePlant(id)
            } catch (e: Exception) {
                Log.e(TAG, "Remote delete failed")
            }
        }
        plantDao.delete(plant)
    }

    suspend fun clearLocalData()
    {
        //cleans the ROOM db
        plantDao.deleteAll()

    }
}