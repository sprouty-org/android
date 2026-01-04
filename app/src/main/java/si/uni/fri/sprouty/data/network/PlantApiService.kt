package si.uni.fri.sprouty.data.network


import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import si.uni.fri.sprouty.data.model.GardenProfileResponse
import si.uni.fri.sprouty.data.model.MasterPlant
import si.uni.fri.sprouty.data.model.PlantIdentificationResponse
import si.uni.fri.sprouty.data.model.UserPlant

interface PlantApiService {

    @Multipart
    @POST("plants/identify")
    suspend fun identifyPlant(
        @Part image: MultipartBody.Part
    ): Response<PlantIdentificationResponse>

    @GET("plants/profile")
    suspend fun getGardenProfile(): Response<GardenProfileResponse>

    @GET("plants/masterPlants")
    suspend fun getRemoteMasterPlants(): List<MasterPlant>

    @GET("plants/userPlants")
    suspend fun getRemoteUserPlants(): List<UserPlant>

    @DELETE("plants/{id}")
    suspend fun deletePlant(
        @Path("id") firebaseId: String?
    ): Response<Unit>

    @POST("plants/connect-sensor")
    suspend fun connectSensor(
        @Query("plantId") plantId: String,
        @Query("sensorId") sensorId: String
    ): Response<Unit>

    @PATCH("plants/{id}/rename")
    suspend fun renamePlant(
        @Path("id") plantId: String?,
        @Query("newName") newName: String
    ): Response<Unit>

    @POST("plants/{id}/disconnect-sensor")
    suspend fun disconnectSensor(
        @Path("id") plantId: String?
    ): Response<Unit>

    @PATCH("plants/{id}/notifications")
    suspend fun toggleNotifications(
        @Path("id") id: String,
        @Query("enabled") enabled: Boolean
    ): Response<Map<String, Any>>

    @POST("plants/{id}/water")
    suspend fun waterPlant(@Path("id") id: String): Response<Unit>
}