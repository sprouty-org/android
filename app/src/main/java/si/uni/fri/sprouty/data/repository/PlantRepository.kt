package si.uni.fri.sprouty.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import si.uni.fri.sprouty.data.database.PlantDao
import si.uni.fri.sprouty.data.model.MasterPlant
import si.uni.fri.sprouty.data.model.Plant
import si.uni.fri.sprouty.data.model.UserPlant
import si.uni.fri.sprouty.data.network.PlantApiService
import si.uni.fri.sprouty.data.model.PlantIdentificationResponse


class PlantRepository(
    private val plantDao: PlantDao,
    private val plantApiService: PlantApiService,
) {
    private val TAG = "PlantRepository"

    private fun parseHumidity(input: String?): Pair<Int?, Int?> {
        if (input.isNullOrBlank()) return Pair(null, null)
        val parts = input.split(",").map { it.trim() }
        val min = parts.getOrNull(0)?.toIntOrNull()
        val max = parts.getOrNull(1)?.toIntOrNull()
        return Pair(min, max)
    }

    private fun mapToEntity(
        userRemote: UserPlant,
        masterPlant: MasterPlant?,
        overrideImageUrl: String? = null
    ): Plant {
        val (minAir, maxAir) = parseHumidity(masterPlant?.airH)
        val (minSoil, maxSoil) = parseHumidity(masterPlant?.soilH)

        return Plant(
            firebaseId = userRemote.id,
            speciesName = userRemote.speciesName ?: "Unknown",
            customName = userRemote.customName ?: userRemote.speciesName,
            imageUrl = overrideImageUrl ?: userRemote.imageUrl,
            lastWatered = userRemote.lastWatered,
            lastSeen = userRemote.lastSeen,
            healthStatus = userRemote.healthStatus ?: "Healthy",
            connectedSensorId = userRemote.connectedSensorId,
            currentHumiditySoil = userRemote.currentHumiditySoil,
            currentTemperature = userRemote.currentTemperature,
            currentHumidityAir = userRemote.currentHumidityAir,
            notificationsEnabled = userRemote.notificationsEnabled,
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
            minAirHumidity = minAir,
            maxAirHumidity = maxAir,
            minSoilHumidity = minSoil,
            maxSoilHumidity = maxSoil,
            targetWateringInterval = userRemote.targetWateringInterval, // Use user's specific interval
            requiredLightLevel = masterPlant?.light ?: "Unknown",
            careDifficulty = masterPlant?.careDifficulty ?: "Unknown"
        )
    }

    fun getAllPlants(): Flow<List<Plant>> = plantDao.getAllPlants()

    /**
     * Optimized Sync: Uses the /profile endpoint to get everything in one call.
     */
    suspend fun syncPlantsFromRemote() {
        try {
            val response = plantApiService.getGardenProfile()
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!

                // Group master data by species name for lookup
                val masterMap = profile.masterPlants.associateBy { it.speciesName }

                val localPlants = profile.userPlants.map { userRemote ->
                    val masterData = masterMap[userRemote.speciesName]
                    mapToEntity(userRemote, masterData)
                }

                plantDao.deleteAll()
                plantDao.insertAll(localPlants)
                Log.d(TAG, "Sync successful: ${localPlants.size} plants.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
        }
    }

    suspend fun identifyAndSavePlant(imagePart: MultipartBody.Part, imageUri: String): PlantIdentificationResponse? {
        return try {
            val response = plantApiService.identifyPlant(imagePart)
            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                val localPlant = mapToEntity(result.userPlant, result.masterPlant, imageUri)
                plantDao.insert(localPlant)
                result
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Identification failed: ${e.message}")
            null
        }
    }

    // --- Action Methods ---

    suspend fun updatePlantName(plantId: String?, newName: String): Boolean {
        return try {
            val response = plantApiService.renamePlant(plantId, newName)
            if (response.isSuccessful) {
                plantDao.updatePlantName(plantId, newName)
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Rename failed: ${e.message}")
            false
        }
    }

    suspend fun connectSensor(plantId: String, sensorId: String): Boolean {
        return try {
            val response = plantApiService.connectSensor(plantId, sensorId)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Sensor connect failed: ${e.message}")
            false
        }
    }

    suspend fun disconnectSensor(plantId: String?): Boolean {
        return try {
            val response = plantApiService.disconnectSensor(plantId)
            if (response.isSuccessful) {
                plantDao.clearSensorId(plantId)
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Sensor disconnect failed: ${e.message}")
            false
        }
    }

    suspend fun deletePlant(plant: Plant) {
        try {
            val response = plantApiService.deletePlant(plant.firebaseId)
            if (response.isSuccessful) plantDao.delete(plant)
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed: ${e.message}")
        }
    }

    suspend fun clearLocalData() { plantDao.deleteAll() }
}