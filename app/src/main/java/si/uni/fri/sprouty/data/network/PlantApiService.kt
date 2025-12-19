package si.uni.fri.sprouty.data.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import si.uni.fri.sprouty.data.model.PlantIdentificationResponse
import si.uni.fri.sprouty.data.model.UserPlant

interface PlantApiService {

    @Multipart
    @POST("plants/identify")
    suspend fun identifyPlant(
        @Header("X-User-Id") userId: String,
        @Part image: MultipartBody.Part
    ): Response<PlantIdentificationResponse>

    @GET("plants")
    suspend fun getRemotePlants(
        @Header("X-User-Id") userId: String
    ): List<UserPlant>

    @DELETE("plants/{id}")
    suspend fun deletePlant(
        @Header("X-User-Id") userId: String,
        @Path("id") firebaseId: String
    ): Response<Unit>

    @PUT("plants/{id}")
    suspend fun updateUserPlant(
        @Header("X-User-Id") userId: String,
        @Path("id") firebaseId: String,
        @Body userPlant: UserPlant
    ): Response<Unit>
}