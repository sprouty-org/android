package si.uni.fri.sprouty.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import si.uni.fri.sprouty.data.model.Plant

@Dao
interface PlantDao {

    @Query("SELECT * FROM plants ORDER BY localId ASC")
    fun getAllPlants(): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE firebaseId = :firebaseId LIMIT 1")
    suspend fun getPlantByFirebaseId(firebaseId: String): Plant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plant: Plant): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plants: List<Plant>)

    @Update
    suspend fun update(plant: Plant)

    @Query("UPDATE plants SET customName = :newName WHERE firebaseId = :plantId")
    suspend fun updatePlantName(plantId: String?, newName: String)

    @Query("UPDATE plants SET connectedSensorId = NULL WHERE firebaseId = :plantId")
    suspend fun clearSensorId(plantId: String?)

    @Delete
    suspend fun delete(plant: Plant)

    @Query("DELETE FROM plants")
    suspend fun deleteAll()
}