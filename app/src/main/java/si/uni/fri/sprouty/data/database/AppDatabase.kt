package si.uni.fri.sprouty.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import si.uni.fri.sprouty.data.model.Plant // Updated import

// Define all entities here. Increment the version number whenever you change the schema.
@Database(entities = [Plant::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Define the access point for the DAO
    abstract fun plantDao(): PlantDao // Refers to the new PlantDao.kt

    // Companion object for Singleton Pattern (Will be replaced by DI)
    companion object {
        // ... getDatabase implementation remains the same for now ...
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sprouty_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}