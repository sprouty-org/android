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
import si.uni.fri.sprouty.data.model.parseError

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
            targetWateringInterval = userRemote.targetWateringInterval,
            requiredLightLevel = masterPlant?.light ?: "Unknown",
            careDifficulty = masterPlant?.careDifficulty ?: "Unknown"
        )
    }

    fun getAllPlants(): Flow<List<Plant>> = plantDao.getAllPlants()


    suspend fun syncPlantsFromRemote(): Result<Unit> {
        return try {
            val response = plantApiService.getGardenProfile()
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                val masterMap = profile.masterPlants.associateBy { it.speciesName }

                val localPlants = profile.userPlants.map { userRemote ->
                    val masterData = masterMap[userRemote.speciesName]
                    mapToEntity(userRemote, masterData)
                }

                plantDao.deleteAll()
                plantDao.insertAll(localPlants)
                Log.d(TAG, "Sync successful: ${localPlants.size} plants.")
                Result.success(Unit)
            } else {
                val error = response.parseError()
                Result.failure(Exception(error?.message ?: "Sync failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun identifyAndSavePlant(
        imagePart: MultipartBody.Part,
        imageUri: String
    ): Result<PlantIdentificationResponse> {
        return try {
            val response = plantApiService.identifyPlant(imagePart)
            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                val localPlant = mapToEntity(result.userPlant, result.masterPlant, imageUri)
                plantDao.insert(localPlant)
                Result.success(result)
            } else {
                val errorObj = response.parseError()
                val message = errorObj?.message ?: "Server Error: ${response.code()}"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Identification failed: ${e.message}")
            Result.failure(e)
        }
    }

    // --- Action Methods ---

    suspend fun updatePlantName(plantId: String?, newName: String): Result<Unit> {
        return try {
            val response = plantApiService.renamePlant(plantId, newName)
            if (response.isSuccessful) {
                plantDao.updatePlantName(plantId, newName)
                Result.success(Unit)
            } else {
                val error = response.parseError()
                Result.failure(Exception(error?.message ?: "Rename failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Rename failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun connectSensor(plantId: String, sensorId: String): Result<Unit> {
        return try {
            val response = plantApiService.connectSensor(plantId, sensorId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val error = response.parseError()
                Result.failure(Exception(error?.message ?: "Connection failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sensor connect failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun disconnectSensor(plantId: String?): Result<Unit> {
        return try {
            val response = plantApiService.disconnectSensor(plantId)
            if (response.isSuccessful) {
                plantDao.clearSensorId(plantId)
                Result.success(Unit)
            } else {
                val error = response.parseError()
                Result.failure(Exception(error?.message ?: "Disconnect failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sensor disconnect failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deletePlant(plant: Plant): Result<Unit> {
        return try {
            val response = plantApiService.deletePlant(plant.firebaseId)
            if (response.isSuccessful) {
                plantDao.delete(plant)
                Result.success(Unit)
            } else {
                val error = response.parseError()
                Result.failure(Exception(error?.message ?: "Delete failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun clearLocalData() { plantDao.deleteAll() }
}