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
import si.uni.fri.sprouty.data.model.parseError
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil

class FirebaseUtils(
    private val authApiService: AuthApiService,
    private val sharedPreferencesUtil: SharedPreferencesUtil,
    private val plantRepository: PlantRepository? = null
) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val TAG = "FirebaseUtils"

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
                        val result = exchangeToken("users/register/google", firebaseIdToken, fcmToken)

                        result.onSuccess { authResponse ->
                            sharedPreferencesUtil.saveUser(
                                jwt = authResponse.token,
                                name = name,
                                firebaseUid = authResponse.firebaseUid
                            )
                            withContext(Dispatchers.Main) { onSuccess() }
                        }.onFailure { error ->
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                                onFailure()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google Registration error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Sign-Up failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onFailure()
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
                    val result = exchangeToken("users/login/google", firebaseIdToken, fcmToken)

                    result.onSuccess { authResponse ->
                        sharedPreferencesUtil.saveUser(
                            jwt = authResponse.token,
                            name = name,
                            firebaseUid = authResponse.firebaseUid
                        )
                        withContext(Dispatchers.Main) { onSuccess() }
                    }.onFailure { error ->
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                            onFailure()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google Login error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Login failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onFailure()
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
                        val result = exchangeToken("users/login/email", firebaseIdToken, fcmToken)

                        result.onSuccess { authResponse ->
                            sharedPreferencesUtil.saveUser(
                                jwt = authResponse.token,
                                name = firebaseUser.displayName ?: "",
                                firebaseUid = authResponse.firebaseUid
                            )
                            withContext(Dispatchers.Main) { onSuccess() }
                        }.onFailure { error ->
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                                onFailure()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Email login error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Login failed: Invalid credentials.", Toast.LENGTH_SHORT).show()
                    onFailure()
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
                val response = authApiService.register(RegisterRequest(email, pass, name, fcmToken))

                if (response.isSuccessful && response.body() != null) {
                    firebaseAuth.signInWithEmailAndPassword(email, pass).await()
                    sharedPreferencesUtil.saveUser(
                        jwt = response.body()!!.token,
                        name = name,
                        firebaseUid = response.body()!!.firebaseUid
                    )
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    val errorMsg = response.parseError()?.message ?: "Registration failed."
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        onFailure()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration error: ${e.message}")
                withContext(Dispatchers.Main) { onFailure() }
            }
        }
    }

    private suspend fun exchangeToken(endpointPath: String, idToken: String, fcmToken: String?): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = GoogleLoginRequest(idToken, fcmToken)
                val response = authApiService.exchangeToken(endpointPath, request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val error = response.parseError()
                    Result.failure(Exception(error?.message ?: "Auth Exchange Failed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exchange error: ${e.message}")
                Result.failure(e)
            }
        }
    }

    fun logout(scope: CoroutineScope, onLogoutSuccess: () -> Unit) {
        scope.launch {
            try {
                firebaseAuth.signOut()
                withContext(Dispatchers.IO) {
                    plantRepository?.clearLocalData()
                }
                sharedPreferencesUtil.clearUser()
                withContext(Dispatchers.Main) { onLogoutSuccess() }
            } catch (e: Exception) {
                Log.e(TAG, "Logout error: ${e.message}")
                withContext(Dispatchers.Main) { onLogoutSuccess() }
            }
        }
    }

    fun deleteUser(context: Context, scope: CoroutineScope, onComplete: () -> Unit) {
        scope.launch {
            try {
                authApiService.deleteAccount()
                withContext(Dispatchers.IO) {
                    plantRepository?.clearLocalData()
                }
                sharedPreferencesUtil.clearUser()
                try {
                    firebaseAuth.currentUser?.delete()?.await()
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase auth delete failed: ${e.message}")
                }
                firebaseAuth.signOut()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Critical error during deletion: ${e.message}")
                sharedPreferencesUtil.clearUser()
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }
}