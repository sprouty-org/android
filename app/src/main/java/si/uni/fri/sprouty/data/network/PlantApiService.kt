package si.uni.fri.sprouty.data.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import si.uni.fri.sprouty.data.model.MasterPlant
import si.uni.fri.sprouty.data.model.PlantIdentificationResponse
import si.uni.fri.sprouty.data.model.UserPlant

interface PlantApiService {

    @Multipart
    @POST("plants/identify")
    suspend fun identifyPlant(
        @Part image: MultipartBody.Part
    ): Response<PlantIdentificationResponse>

    @GET("plants/master")
    suspend fun getRemoteMasterPlants(): List<MasterPlant>

    @GET("plants/user")
    suspend fun getRemoteUserPlants(): List<UserPlant>

    @DELETE("plants/{id}")
    suspend fun deletePlant(
        @Path("id") firebaseId: String
    ): Response<Unit>

    @PUT("plants/{id}")
    suspend fun updateUserPlant(
        @Path("id") firebaseId: String,
        @Body userPlant: UserPlant
    ): Response<Unit>
}