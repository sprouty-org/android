package si.uni.fri.sprouty.data.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import si.uni.fri.sprouty.data.model.Plant // Updated import
import si.uni.fri.sprouty.data.model.PlantSyncResponse // Updated import

/**
 * Retrofit interface for interacting with the Spring Boot 'plant-service' endpoints.
 * NOTE: Manual JWT token headers have been REMOVED as AuthInterceptor handles this now.
 */
interface PlantApiService {

    @GET("plants/user")
    suspend fun getRemotePlants(): List<Plant> // CLEANED: No @Header("Authorization")

    @POST("plants/sync")
    suspend fun syncPlant(
        @Body plant: Plant
    ): PlantSyncResponse // CLEANED: No @Header("Authorization")

    @DELETE("plants/{firebaseId}")
    suspend fun deletePlant(
        @Path("firebaseId") firebaseId: String
    ) // CLEANED: No @Header("Authorization")
}