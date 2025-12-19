package si.uni.fri.sprouty.data.repository

import android.util.Log
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
    suspend fun identifyAndSavePlant(imagePart: MultipartBody.Part): PlantIdentificationResponse? {
        val response = plantApiService.identifyPlant(imagePart)
        if (response.isSuccessful && response.body() != null) {
            val result = response.body()!!

            val localPlant = Plant(
                firebaseId = result.userPlant.id,
                speciesName = result.userPlant.speciesName ?: "Unknown",
                customName = result.userPlant.customName,
                plantedDate = result.userPlant.plantedDate,
                lastWatered = result.userPlant.lastWatered,
                healthStatus = result.userPlant.healthStatus ?: "Healthy",
                currentGrowthStage = result.userPlant.currentGrowthStage ?: "New",
                targetWateringInterval = result.userPlant.targetWateringInterval,
                requiredLightLevel = result.userPlant.requiredLightLevel ?: "Bright Indirect",
                notificationsEnabled = true // Default for new plants
            )

            plantDao.insert(localPlant)
            return result
        }
        return null
    }

    // --- 3. REMOTE SYNC (PULL) ---

    suspend fun syncPlantsFromRemote(userId: String?) {
        Log.d(TAG, "Starting remote sync for user: $userId")
        try {
            // 1. Fetch from Spring Boot API
            val remoteUserPlants = plantApiService.getRemotePlants()

            // 2. Map remote list (UserPlant) to local Room list (Plant)
            val localPlants = remoteUserPlants.map { remote ->
                Plant(
                    // IDs
                    localId = 0, // Room will auto-generate this because it's the PrimaryKey
                    firebaseId = remote.id,
                    speciesName = remote.speciesName ?: "Unknown",

                    // Customization
                    customName = remote.customName ?: remote.speciesName,
                    plantedDate = remote.plantedDate,
                    lastWatered = remote.lastWatered,
                    userPictureUrl = null, // Backend doesn't store this yet

                    // Health & Status (Defaults if null from server)
                    healthStatus = remote.healthStatus ?: "Healthy",
                    currentGrowthStage = remote.currentGrowthStage ?: "N/A",
                    notes = null,

                    // Sensor & Notifications
                    notificationsEnabled = true,
                    connectedSensorId = null,
                    lastSensorDataUrl = null,

                    // Requirements
                    targetWateringInterval = remote.targetWateringInterval,
                    requiredLightLevel = remote.requiredLightLevel ?: "Unknown"
                )
            }

            // 3. Clear and Refresh Local Cache
            plantDao.deleteAll()
            plantDao.insertAll(localPlants)

            Log.d(TAG, "Sync complete. Inserted ${localPlants.size} plants from remote.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync plants from remote server: ${e.message}")
            e.printStackTrace()
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