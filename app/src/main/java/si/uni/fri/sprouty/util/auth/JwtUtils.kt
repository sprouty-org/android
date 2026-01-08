package si.uni.fri.sprouty.util.auth

import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import si.uni.fri.sprouty.data.network.AuthApiService
import si.uni.fri.sprouty.data.network.GoogleLoginRequest
import si.uni.fri.sprouty.data.model.parseError
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil

class JwtUtils(
    private val authApiService: AuthApiService,
    private val sharedPreferencesUtil: SharedPreferencesUtil
) {
    private val TAG = "JwtUtils"

    fun isExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true

            val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val payload = JSONObject(payloadJson)

            val exp = payload.getLong("exp")
            val now = System.currentTimeMillis() / 1000
            now >= exp - 3600
        } catch (e: Exception) {
            true
        }
    }

    suspend fun refreshJwtToken(): Boolean {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            try {
                val firebaseToken = firebaseUser.getIdToken(true).await()?.token ?: return false
                val response = authApiService.loginWithGoogle(GoogleLoginRequest(firebaseToken))

                if (response.isSuccessful && response.body() != null) {
                    val newJwt = response.body()!!.token
                    sharedPreferencesUtil.saveToken(newJwt)
                    Log.d(TAG, "JWT refreshed successfully.")
                    return true
                } else {
                    val error = response.parseError()
                    Log.e(TAG, "JWT refresh failed: ${error?.message}")
                    return false
                }
            } catch (e: Exception) {
                Log.e(TAG, "JWT refresh network exception: ${e.message}")
                return false
            }
        }
        return false
    }
}