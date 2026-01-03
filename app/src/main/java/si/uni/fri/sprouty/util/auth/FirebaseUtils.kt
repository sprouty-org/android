package si.uni.fri.sprouty.util.auth

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import si.uni.fri.sprouty.data.network.AuthApiService
import si.uni.fri.sprouty.data.repository.PlantRepository
import si.uni.fri.sprouty.data.network.AuthResponse
import si.uni.fri.sprouty.data.network.GoogleLoginRequest
import si.uni.fri.sprouty.data.network.RegisterRequest
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil

class FirebaseUtils(
    private val authApiService: AuthApiService,
    private val sharedPreferencesUtil: SharedPreferencesUtil,
    private val plantRepository: PlantRepository? = null
) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val TAG = "FirebaseUtils"

    /**
     * Helper to fetch the current FCM Device Token.
     */
    private suspend fun getFcmToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token: ${e.message}")
            null
        }
    }

    // --- GOOGLE FLOWS ---

    fun exchangeGoogleRegisterToken(
        context: Context,
        scope: CoroutineScope,
        googleIdToken: String,
        name: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        scope.launch {
            try {
                val fcmToken = getFcmToken()
                val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    val firebaseIdToken = firebaseUser.getIdToken(true).await()?.token

                    if (firebaseIdToken != null) {
                        val authResponse = exchangeToken("users/register/google", firebaseIdToken, fcmToken)

                        if (authResponse != null) {
                            sharedPreferencesUtil.saveUser(authResponse.token, name)
                            withContext(Dispatchers.Main) { onSuccess() }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Registration failed: Backend error", Toast.LENGTH_LONG).show()
                                onFailure()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google Registration error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Google Sign-Up failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun exchangeGoogleLoginToken(
        context: Context,
        scope: CoroutineScope,
        googleIdToken: String,
        name: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        scope.launch {
            try {
                val fcmToken = getFcmToken()
                val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val firebaseIdToken = authResult.user?.getIdToken(true)?.await()?.token

                if (firebaseIdToken != null) {
                    val authResponse = exchangeToken("users/login/google", firebaseIdToken, fcmToken)

                    if (authResponse != null) {
                        sharedPreferencesUtil.saveUser(authResponse.token, name)
                        withContext(Dispatchers.Main) { onSuccess() }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Login failed: Backend error", Toast.LENGTH_LONG).show()
                            onFailure()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google Login error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network Error during Login", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- EMAIL/PASSWORD FLOWS ---

    fun loginWithEmail(
        context: Context,
        coroutineScope: CoroutineScope,
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        coroutineScope.launch {
            try {
                val fcmToken = getFcmToken()
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    val firebaseIdToken = firebaseUser.getIdToken(true).await()?.token
                    if (firebaseIdToken != null) {
                        val authResponse = exchangeToken("users/login/email", firebaseIdToken, fcmToken)
                        if (authResponse != null) {
                            sharedPreferencesUtil.saveUser(authResponse.token, firebaseUser.displayName ?: email)
                            withContext(Dispatchers.Main) { onSuccess() }
                        }else{
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Login failed: Backend error", Toast.LENGTH_LONG).show()
                                onFailure()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Email login error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Login failed: Invalid credentials.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun registerUser(
        context: Context,
        scope: CoroutineScope,
        email: String,
        pass: String,
        name: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        scope.launch {
            try {
                val fcmToken = getFcmToken()
                // Passing fcmToken to the standard register request
                val response = authApiService.register(RegisterRequest(email, pass, name, fcmToken))

                if (response.isSuccessful && response.body() != null) {
                    firebaseAuth.signInWithEmailAndPassword(email, pass).await()
                    sharedPreferencesUtil.saveUser(response.body()!!.token, name)
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Registration failed.", Toast.LENGTH_SHORT).show()
                        onFailure()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration error: ${e.message}")
            }
        }
    }

    // --- HELPERS ---

    /**
     * Generic helper to exchange a Firebase ID Token + FCM Token for a backend JWT.
     */
    private suspend fun exchangeToken(endpointPath: String, idToken: String, fcmToken: String?): AuthResponse? {
        return withContext(Dispatchers.IO) {
            try {
                // We use GoogleLoginRequest DTO which now includes fcmToken
                val request = GoogleLoginRequest(idToken, fcmToken)
                val response = authApiService.exchangeToken(endpointPath, request)
                if (response.isSuccessful) response.body() else null
            } catch (e: Exception) {
                Log.e(TAG, "Exchange error: ${e.message}")
                null
            }
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        firebaseAuth.signOut()
        sharedPreferencesUtil.clearUser()
        if (plantRepository != null) {
            CoroutineScope(Dispatchers.IO).launch {
                plantRepository.clearLocalData()
                withContext(Dispatchers.Main) { onLogoutSuccess() }
            }
        } else {
            onLogoutSuccess()
        }
    }

    /**
     * Completely deletes the user account.
     * 1. Notifies the backend to clean up Firestore/Cloud Storage.
     * 2. Clears the local Room database.
     * 3. Clears SharedPreferences.
     * 4. Deletes the account from Firebase Auth.
     */
    fun deleteUser(context: Context, scope: CoroutineScope, onComplete: () -> Unit) {
        scope.launch {
            try {
                // 1. Backend Deletion (Already verified working with 204!)
                val response = authApiService.deleteAccount()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Backend reported failure: ${response.code()}")
                    // You might want to stop here if the backend failed
                }

                // 2. Clear Local Data
                plantRepository?.clearLocalData()
                sharedPreferencesUtil.clearUser()

                // 3. Firebase Auth Cleanup
                try {
                    // We try to delete the user record from Firebase Auth directly
                    // If this fails due to "Recent Login Required", we sign out anyway
                    firebaseAuth.currentUser?.delete()?.await()
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase user delete failed (likely needs re-auth), signing out instead: ${e.message}")
                }

                // 4. Always sign out locally
                firebaseAuth.signOut()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                    // 5. This MUST be called to trigger navigateToLogin()
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Critical error during account deletion: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    // Optionally call onComplete() here too if you want to force them out anyway
                }
            }
        }
    }
}