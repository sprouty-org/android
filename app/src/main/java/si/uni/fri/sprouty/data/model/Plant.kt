package si.uni.fri.sprouty.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val firebaseId: String? = null,
    val speciesName: String,
    val customName: String? = null,
    val imageUrl: String? = null,
    val healthStatus: String,
    val lastWatered: Long,

    // --- Sensor Readings ---
    val currentHumiditySoil: Double = 0.0,
    val currentTemperature: Double? = null,
    val currentHumidityAir: Double? = null,

    val botanicalFact: String? = null,
    val toxicity: String? = null,
    val growthHabit: String? = null,
    val soilType: String? = null,
    val botanicalType: String? = null,
    val lifecycle: String? = null,
    val fruitInfo: String? = null,
    val uses: List<String>? = null,
    val maxHeight: Int? = null,
    val minTemp: Int? = null,
    val maxTemp: Int? = null,
    val minAirHumidity: Int? = null,
    val maxAirHumidity: Int? = null,
    val minSoilHumidity: Int? = null,
    val maxSoilHumidity: Int? = null,
    val targetWateringInterval: Int,
    val requiredLightLevel: String,
    val notificationsEnabled: Boolean = true,
    val connectedSensorId: String? = null,
    val careDifficulty: String,
    val lastSeen: Long = 0,
)

data class UserPlant(
    val id: String? = null,
    val ownerId: String? = null,
    val speciesId: String? = null,
    val speciesName: String? = null,
    val customName: String? = null,

    // --- Sensor Readings ---
    val currentHumiditySoil: Double = 0.0,
    val currentTemperature: Double? = null,
    val currentHumidityAir: Double? = null,

    val imageUrl: String? = null,

    val lastWatered: Long = 0,
    val targetWateringInterval: Int,
    val healthStatus: String? = null,
    val lastSeen: Long = 0,


    val connectedSensorId: String? = null,
    val notificationsEnabled: Boolean = true
)

data class MasterPlant(
    val speciesName: String,
    val type: String,
    val life: String,
    val fruit: String,
    val uses: List<String>,
    val fact: String,
    val tox: String,
    val minT: Int,
    val maxT: Int,
    val light: String,
    val soilH: String,
    val airH: String,
    val waterInterval: Int,
    val growth: String,
    val soil: String,
    val maxHeight: Int,
    val careDifficulty: String
)