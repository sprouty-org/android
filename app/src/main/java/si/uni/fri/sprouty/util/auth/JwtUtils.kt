package si.uni.fri.sprouty.util.auth

import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import si.uni.fri.sprouty.data.network.AuthApiService // Updated import
import si.uni.fri.sprouty.data.network.GoogleLoginRequest
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil // Updated import

/**
 * Utility class for handling JWT-related logic like expiration and refresh.
 *
 * NOTE: Converted to a class to be injectable, taking its dependencies in the constructor.
 */
class JwtUtils(
    private val authApiService: AuthApiService, // Injected dependency
    private val sharedPreferencesUtil: SharedPreferencesUtil // Injected dependency
) {
    private val TAG = "JwtUtils"

    /**
     * Checks if the token is expired (or expires in the next hour).
     */
    fun isExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true

            val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val payload = JSONObject(payloadJson)

            val exp = payload.getLong("exp")  // seconds
            val now = System.currentTimeMillis() / 1000

            // Token is considered "expired" if it is already past or expires in the next hour (3600 seconds)
            now >= exp - 3600
        } catch (e: Exception) {
            true // treat malformed tokens as expired
        }
    }

    /**
     * Attempts to refresh the JWT using the Firebase ID token.
     * Requires Android Context as a parameter only for accessing FirebaseAuth.getInstance()
     * and as a historical dependency in the interceptor flow.
     */
    suspend fun refreshJwtToken(): Boolean {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            try {
                // 1. Fresh Firebase ID token
                val firebaseToken = firebaseUser.getIdToken(true).await()?.token ?: return false

                // 2. Exchange Firebase token → new JWT using injected service
                val response = authApiService.loginWithGoogle(GoogleLoginRequest(firebaseToken))

                if (response.isSuccessful && response.body() != null) {
                    val newJwt = response.body()!!.token
                    sharedPreferencesUtil.saveToken(newJwt) // Use decoupled utility
                    Log.d(TAG, "JWT refreshed successfully.")
                    return true
                } else {
                    Log.e(TAG, "JWT refresh failed. API response not successful or body null.")
                    return false
                }
            } catch (e: Exception) {
                Log.e(TAG, "JWT refresh network exception: ${e.message}")
                e.printStackTrace()
                return false
            }
        } else {
            Log.d(TAG, "Firebase user is null, cannot refresh token.")
            return false
        }
    }
}