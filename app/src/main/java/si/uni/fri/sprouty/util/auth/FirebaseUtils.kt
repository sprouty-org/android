package si.uni.fri.sprouty.util.auth

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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

    // --- GOOGLE FLOWS ---

    /**
     * Handles Google Registration:
     * 1. Signs into Firebase locally with the Google Credential.
     * 2. Retrieves a Firebase ID Token (Audience: sprouty-plantapp).
     * 3. Sends that Firebase Token to the backend.
     */
    fun exchangeGoogleRegisterToken(
        context: Context,
        scope: CoroutineScope,
        googleIdToken: String,
        name: String,
        onSuccess: () -> Unit
    ) {
        scope.launch {
            try {
                // 1. Convert Google Token to Firebase Session locally
                val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    // 2. Get the Firebase ID Token (This fixes the 'aud' error)
                    val firebaseIdToken = firebaseUser.getIdToken(true).await()?.token

                    if (firebaseIdToken != null) {
                        // 3. Send the FIREBASE token to backend
                        val authResponse = exchangeToken("users/register/google", firebaseIdToken)

                        if (authResponse != null) {
                            sharedPreferencesUtil.saveUser(authResponse.token, name)
                            withContext(Dispatchers.Main) { onSuccess() }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Registration failed (Backend rejected token).", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google Registration error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Google Sign-Up failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Handles Google Login:
     * Exact same logic as registration, ensuring a Firebase ID Token is sent to the backend.
     */
    fun exchangeGoogleLoginToken(
        context: Context,
        scope: CoroutineScope,
        googleIdToken: String,
        name: String,
        onSuccess: () -> Unit
    ) {
        scope.launch {
            try {
                // 1. Local Firebase Sign-in
                val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()

                // 2. Extract Firebase ID Token
                val firebaseIdToken = authResult.user?.getIdToken(true)?.await()?.token

                if (firebaseIdToken != null) {
                    // 3. Send to backend
                    val authResponse = exchangeToken("users/login/google", firebaseIdToken)

                    if (authResponse != null) {
                        sharedPreferencesUtil.saveUser(authResponse.token, name)
                        withContext(Dispatchers.Main) { onSuccess() }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Login failed: Backend error", Toast.LENGTH_LONG).show()
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
        onSuccess: () -> Unit
    ) {
        coroutineScope.launch {
            try {
                // 1. Authenticate with Firebase first
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    // 2. Retrieve Firebase ID Token
                    val firebaseIdToken = firebaseUser.getIdToken(true).await()?.token
                    if (firebaseIdToken != null) {
                        val authResponse = exchangeToken("users/login/email", firebaseIdToken)
                        if (authResponse != null) {
                            sharedPreferencesUtil.saveUser(authResponse.token, firebaseUser.displayName ?: email)
                            withContext(Dispatchers.Main) { onSuccess() }
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
        onSuccess: () -> Unit
    ) {
        scope.launch {
            try {
                // 1. Create in Backend first
                val response = authApiService.register(RegisterRequest(email, pass, name))
                if (response.isSuccessful && response.body() != null) {
                    // 2. Sign in locally to Firebase to sync the session
                    firebaseAuth.signInWithEmailAndPassword(email, pass).await()
                    sharedPreferencesUtil.saveUser(response.body()!!.token, name)
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Registration failed.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration error: ${e.message}")
            }
        }
    }

    // --- HELPERS ---

    private suspend fun exchangeToken(endpointPath: String, token: String): AuthResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val request = GoogleLoginRequest(token)
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
}