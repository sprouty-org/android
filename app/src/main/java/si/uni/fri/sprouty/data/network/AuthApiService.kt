package si.uni.fri.sprouty.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Url

// --- Data Transfer Objects ---

data class UpdateFcmRequest(
    val fcmToken: String
)

data class GoogleLoginRequest(
    val idToken: String,
    val fcmToken: String? = null
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val fcmToken: String? = null
)


data class AuthResponse(
    val firebaseUid: String,
    val token: String
)

interface AuthApiService {

    // --- Authentication ---

    @PATCH("users/update-fcm")
    suspend fun updateFcmToken(@Body request: UpdateFcmRequest): Response<Unit>

    @POST("users/login/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @POST("users/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @DELETE("users/me")
    suspend fun deleteAccount(): Response<Unit>

    @POST
    suspend fun exchangeToken(
        @Url url: String,
        @Body request: GoogleLoginRequest
    ): Response<AuthResponse>
}