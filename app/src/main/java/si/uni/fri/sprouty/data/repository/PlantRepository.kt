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

    private fun parseHumidity(input: String?): Pair<Int?, Int?> {
        if (input.isNullOrBlank()) return Pair(null, null)
        val parts = input.split(",").map { it.trim() }
        val min = parts.getOrNull(0)?.toIntOrNull()
        val max = parts.getOrNull(1)?.toIntOrNull()
        return Pair(min, max)
    }

    fun getAllPlants(): Flow<List<Plant>> = plantDao.getAllPlants()

    suspend fun identifyAndSavePlant(imagePart: MultipartBody.Part, imageUri: String): PlantIdentificationResponse? {
        return try {
            val response = plantApiService.identifyPlant(imagePart)
            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!

                val (minAir, maxAir) = parseHumidity(result.masterPlant.airH)
                val (minSoil, maxSoil) = parseHumidity(result.masterPlant.soilH)

                val localPlant = Plant(
                    firebaseId = result.userPlant.id,
                    speciesName = result.userPlant.speciesName ?: "Unknown",
                    customName = result.userPlant.customName,
                    lastWatered = result.userPlant.lastWatered,
                    healthStatus = result.userPlant.healthStatus ?: "Healthy",
                    connectedSensorId = result.userPlant.connectedSensorId,

                    // --- Added New Sensor Fields ---
                    currentHumiditySoil = result.userPlant.currentHumiditySoil,
                    currentTemperature = result.userPlant.currentTemperature,
                    currentHumidityAir = result.userPlant.currentHumidityAir,

                    notificationsEnabled = result.userPlant.notificationsEnabled,
                    imageUrl = imageUri,
                    botanicalFact = result.masterPlant.fact,
                    toxicity = result.masterPlant.tox,
                    growthHabit = result.masterPlant.growth,
                    soilType = result.masterPlant.soil,
                    botanicalType = result.masterPlant.type,
                    lifecycle = result.masterPlant.life,
                    fruitInfo = result.masterPlant.fruit,
                    uses = result.masterPlant.uses,
                    maxHeight = result.masterPlant.maxHeight,
                    minTemp = result.masterPlant.minT,
                    maxTemp = result.masterPlant.maxT,
                    minAirHumidity = minAir,
                    maxAirHumidity = maxAir,
                    minSoilHumidity = minSoil,
                    maxSoilHumidity = maxSoil,
                    targetWateringInterval = result.masterPlant.waterInterval,
                    requiredLightLevel = result.masterPlant.light
                )

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
                val userPlantsDeferred = async { plantApiService.getRemoteUserPlants() }
                val masterPlantsDeferred = async { plantApiService.getRemoteMasterPlants() }

                val remoteUserPlants = userPlantsDeferred.await()
                val remoteMasterData = masterPlantsDeferred.await()
                val masterMap = remoteMasterData.associateBy { it.speciesName }

                val localPlants = remoteUserPlants.map { userRemote ->
                    val masterPlant = masterMap[userRemote.speciesName]
                    val (minAir, maxAir) = parseHumidity(masterPlant?.airH)
                    val (minSoil, maxSoil) = parseHumidity(masterPlant?.soilH)

                    Plant(
                        localId = 0,
                        firebaseId = userRemote.id,
                        speciesName = userRemote.speciesName ?: "Unknown",
                        customName = userRemote.customName ?: userRemote.speciesName,
                        imageUrl = userRemote.imageUrl,
                        lastWatered = userRemote.lastWatered,
                        healthStatus = userRemote.healthStatus ?: "Healthy",
                        connectedSensorId = userRemote.connectedSensorId,

                        // --- Added New Sensor Fields ---
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
                        targetWateringInterval = masterPlant?.waterInterval ?: 7,
                        requiredLightLevel = masterPlant?.light ?: "Unknown"
                    )
                }

                plantDao.deleteAll()
                plantDao.insertAll(localPlants)
                Log.d(TAG, "Sync successful: ${localPlants.size} plants updated.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync: ${e.message}")
        }
    }

    suspend fun connectSensor(plantId: String, sensorId: String): Boolean {
        return try {
            val response = plantApiService.connectSensor(plantId, sensorId)
            if (response.isSuccessful) {
                syncPlantsFromRemote()
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect sensor: ${e.message}")
            false
        }
    }

    suspend fun deletePlant(plant: Plant) {
        plant.firebaseId?.let { id ->
            try { plantApiService.deletePlant(id) } catch (_: Exception) { Log.e(TAG, "Remote delete failed") }
        }
        plantDao.delete(plant)
    }

    suspend fun clearLocalData() { plantDao.deleteAll() }
}