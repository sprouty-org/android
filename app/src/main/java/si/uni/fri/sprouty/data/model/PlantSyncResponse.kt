package si.uni.fri.sprouty.data.model

/**
 * Defines the expected response structure after syncing a plant.
 * The server usually returns the created/updated resource, often including the official
 * firebaseId and a success flag.
 */
data class PlantSyncResponse(
    val firebaseId: String,
    val success: Boolean,
    val message: String? = null
    // You might return the full Plant object here if needed
)