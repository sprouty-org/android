package si.uni.fri.sprouty.data.repository

import android.util.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import si.uni.fri.sprouty.data.database.PlantDao
import si.uni.fri.sprouty.data.model.MasterPlant
import si.uni.fri.sprouty.data.model.Plant
import si.uni.fri.sprouty.data.model.PlantIdentificationResponse
import si.uni.fri.sprouty.data.model.UserPlant
import si.uni.fri.sprouty.data.network.PlantApiService


class PlantRepository(
    private val plantDao: PlantDao,
    private val plantApiService: PlantApiService
) {
    private val TAG = "PlantRepository"

    private fun parseHumidity(input: String?): Pair<Int?, Int?> {
        if (input.isNullOrBlank()) return Pair(null, null)
        val parts = input.split(",").map { it.trim() }
        val min = parts.getOrNull(0)?.toIntOrNull()
        val max = parts.getOrNull(1)?.toIntOrNull()
        return Pair(min, max)
    }

    /**
     * Corrected Mapper: Now handles lastSeen and ensures Long timestamp for lastWatered.
     */
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

            // Timestamps (Long)
            lastWatered = userRemote.lastWatered,
            lastSeen = userRemote.lastSeen, // ADDED: Ensuring sync of last sensor heartbeat

            healthStatus = userRemote.healthStatus ?: "Healthy",
            connectedSensorId = userRemote.connectedSensorId,

            // Sensor Readings (Double)
            currentHumiditySoil = userRemote.currentHumiditySoil,
            currentTemperature = userRemote.currentTemperature,
            currentHumidityAir = userRemote.currentHumidityAir,

            notificationsEnabled = userRemote.notificationsEnabled,

            // Botanical / Master Info
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
            targetWateringInterval = masterPlant?.waterInterval ?: 7,
            requiredLightLevel = masterPlant?.light ?: "Unknown",
            careDifficulty = masterPlant?.careDifficulty ?: "Unknown"
        )
    }

    fun getAllPlants(): Flow<List<Plant>> = plantDao.getAllPlants()

    suspend fun updatePlantName(plantId: String?, newName: String): Boolean {
        return try {
            val response = plantApiService.renamePlant(plantId, newName)
            if (response.isSuccessful) {
                plantDao.updatePlantName(plantId, newName)
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename plant: ${e.message}")
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
            Log.e(TAG, "Failed to disconnect sensor: ${e.message}")
            false
        }
    }

    suspend fun deletePlant(plant: Plant) {
        try {
            val response = plantApiService.deletePlant(plant.firebaseId)
            if (response.isSuccessful) {
                plantDao.delete(plant)
                Log.d(TAG, "Deleted plant ${plant.firebaseId} from remote and local.")
            } else {
                Log.e(TAG, "Remote delete failed with code ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Remote delete failed: ${e.message}")
            plantDao.delete(plant)
        }
    }
    suspend fun identifyAndSavePlant(imagePart: MultipartBody.Part, imageUri: String): PlantIdentificationResponse? {
        return try {
            val response = plantApiService.identifyPlant(imagePart)
            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!

                // Using the mapper!
                val localPlant = mapToEntity(result.userPlant, result.masterPlant, imageUri)

                plantDao.insert(localPlant)
                result
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failure: ${e.message}")
            null
        }
    }

    suspend fun syncPlantsFromRemote() {
        try {
            coroutineScope {
                val userPlants = plantApiService.getRemoteUserPlants()
                val remoteMasterData = plantApiService.getRemoteMasterPlants()
                val masterMap = remoteMasterData.associateBy { it.speciesName }

                val localPlants = userPlants.map { userRemote ->
                    val masterPlant = masterMap[userRemote.speciesName]
                    // Using the mapper!
                    mapToEntity(userRemote, masterPlant)
                }

                plantDao.deleteAll()
                plantDao.insertAll(localPlants)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync: ${e.message}")
        }
    }

    suspend fun connectSensor(plantId: String, sensorId: String): Boolean {
        return try {
            val response = plantApiService.connectSensor(plantId, sensorId)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect sensor: ${e.message}")
            false
        }
    }

    suspend fun clearLocalData() { plantDao.deleteAll() }
}