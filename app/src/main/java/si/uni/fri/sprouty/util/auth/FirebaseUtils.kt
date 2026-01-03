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

    /**
     * Standard Logout:
     * 1. Signs out of Firebase.
     * 2. Clears the local Room database via PlantRepository.
     * 3. Clears the JWT and user info from SharedPreferences.
     */
    fun logout(context: Context, scope: CoroutineScope, onLogoutSuccess: () -> Unit) {
        scope.launch {
            try {
                // 1. Sign out from Firebase first
                firebaseAuth.signOut()

                // 2. Clear Room Database and WAIT for it to finish
                // This is the part that stops the ghosting
                withContext(Dispatchers.IO) {
                    plantRepository?.clearLocalData()
                    Log.d(TAG, "Local database cleared")
                }

                // 3. Clear SharedPreferences
                sharedPreferencesUtil.clearUser()

                // 4. Final Navigation
                withContext(Dispatchers.Main) {
                    onLogoutSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Logout error: ${e.message}")
                withContext(Dispatchers.Main) { onLogoutSuccess() }
            }
        }
    }

    /**
     * Account Deletion:
     * 1. Notifies backend to purge data.
     * 2. Force-clears local Room DB to prevent data ghosting.
     * 3. Deletes Firebase Auth record.
     */
    fun deleteUser(context: Context, scope: CoroutineScope, onComplete: () -> Unit) {
        scope.launch {
            try {
                // 1. Backend Deletion
                val response = authApiService.deleteAccount()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Backend delete failed: ${response.code()}")
                }

                // 2. IMMEDIATE LOCAL CLEARANCE
                // Doing this before the network/auth finish ensures the UI sees 0 plants
                withContext(Dispatchers.IO) {
                    plantRepository?.clearLocalData()
                }
                sharedPreferencesUtil.clearUser()

                // 3. Firebase Auth Cleanup
                try {
                    // This can fail if the user hasn't logged in recently (Security restriction)
                    firebaseAuth.currentUser?.delete()?.await()
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase auth delete failed, proceed with signout: ${e.message}")
                }

                firebaseAuth.signOut()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Account and data deleted", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Critical error during deletion: ${e.message}")
                // If everything fails, at least wipe the local prefs so they can't access the app
                sharedPreferencesUtil.clearUser()
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
}