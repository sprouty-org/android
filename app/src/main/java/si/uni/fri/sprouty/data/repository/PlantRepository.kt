package si.uni.fri.sprouty.data.repository // <--- NEW PACKAGE

import android.util.Log
import kotlinx.coroutines.flow.Flow
import si.uni.fri.sprouty.data.database.PlantDao // <--- NEW IMPORT
import si.uni.fri.sprouty.data.model.Plant // <--- NEW IMPORT
import si.uni.fri.sprouty.data.network.PlantApiService // <--- NEW IMPORT

/**
 * Repository module for handling data operations related to Plant.
 * It abstracts the data sources (Room and Network/Firestore).
 */
class PlantRepository(
    private val plantDao: PlantDao,
    private val plantApiService: PlantApiService // Dependency for talking to the Spring Boot backend
) {
    private val TAG = "PlantRepository"

    // --- 1. LOCAL DATA FLOW (Room) ---

    /**
     * Retrieves all plants from the local Room database.
     * FIX: This function signature matches the PlantDao and is correct.
     */
    fun getAllPlants(): Flow<List<Plant>> {
        // The repository simply returns the Flow exposed by the DAO
        return plantDao.getAllPlants()
    }

    // --- 2. SYNCHRONIZATION AND WRITE OPERATIONS ---

    /**
     * Saves a new plant to the local database and initiates a sync to the remote server.
     */
    suspend fun saveNewPlant(plant: Plant) {
        // Step 1: Save locally for instant UI update and get the localId
        val localId = plantDao.insert(plant)
        Log.d(TAG, "Plant saved locally with ID: $localId")

        // Step 2: Sync to remote server
        val plantForSync = plant.copy(localId = localId)
        syncPlantToRemote(plantForSync)
    }

    /**
     * Internal function to handle the remote sync of a single plant.
     */
    private suspend fun syncPlantToRemote(plant: Plant) {
        try {
            // NOTE: Assuming AuthInterceptor handles the token header.
            val response = plantApiService.syncPlant(plant)

            if (response.success) {
                // Step 3: Update local plant with the official firebaseId from the server
                val updatedPlant = plant.copy(firebaseId = response.firebaseId)
                plantDao.update(updatedPlant)
                Log.d(TAG, "Sync successful. Plant updated with firebaseId: ${response.firebaseId}")
            } else {
                Log.e(TAG, "Remote sync failed: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during sync: ${e.message}")
        }
    }

    /**
     * Pulls the latest list of plants from the server and updates the local cache.
     */
    suspend fun syncPlantsFromRemote() {
        Log.d(TAG, "Starting remote sync...")
        try {
            // NOTE: Assuming AuthInterceptor handles the token header.
            val remotePlants = plantApiService.getRemotePlants()

            // Wipe the local cache and replace it with the fresh data from the server
            plantDao.deleteAll()
            plantDao.insertAll(remotePlants)

            Log.d(TAG, "Sync complete. Inserted ${remotePlants.size} plants.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch plants from remote server: ${e.message}")
        }
    }

    /**
     * Handles the deletion of a plant from both local and remote sources.
     */
    suspend fun deletePlant(plant: Plant) {
        // Step 1: Delete remotely first (if firebaseId exists)
        if (plant.firebaseId != null) {
            try {
                // Assuming successful response is implied by no exception
                plantApiService.deletePlant(plant.firebaseId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete plant remotely: ${e.message}")
            }
        }

        // Step 2: Delete locally
        plantDao.delete(plant)
        Log.d(TAG, "Plant deleted locally.")
    }

    /**
     * Clears all local data.
     */
    suspend fun clearLocalData() {
        plantDao.deleteAll()
        Log.i(TAG, "Local plant cache cleared.")
    }
}