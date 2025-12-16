package si.uni.fri.sprouty.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// --- 1. PLANT ENTITY ---

/**
 * Represents a single user-owned plant instance in the local Room database.
 */
@Entity(tableName = "plants")
data class Plant(
    // --- IDs and References ---
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val firebaseId: String? = null,
    val speciesName: String,

    // --- User Customization & Status ---
    val customName: String? = null,
    val plantedDate: Long,
    val lastWatered: Long,
    val userPictureUrl: String? = null,

    // --- Health & Growth Tracking ---
    val healthStatus: String,
    val currentGrowthStage: String,
    val notes: String? = null,

    // --- Sensor & Notification Data ---
    val notificationsEnabled: Boolean = true,
    val connectedSensorId: String? = null,
    val lastSensorDataUrl: String? = null,

    val targetWateringInterval: Int,
    val requiredLightLevel: String
)

// --- 2. PLANT DAO (Data Access Object) ---