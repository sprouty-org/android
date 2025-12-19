package si.uni.fri.sprouty.util.auth

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
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

    fun exchangeGoogleRegisterToken(
        context: Context,
        scope: CoroutineScope,
        googleIdToken: String,
        name: String,
        onSuccess: () -> Unit
    ) {
        scope.launch {
            try {
                // 1. Send RAW Google Token to backend (Backend extracts email/UID from it)
                val authResponse = exchangeToken("users/register/google", googleIdToken)

                if (authResponse != null) {
                    // 2. Also sign into Firebase locally so the app's Firebase instance is active
                    val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleIdToken, null)
                    firebaseAuth.signInWithCredential(credential).await()

                    // 3. Save internal JWT and proceed
                    sharedPreferencesUtil.saveUser(authResponse.token, name)
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Registration failed (Backend error).", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google Registration error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Google Sign-Up failed.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun exchangeGoogleLoginToken(
        context: Context,
        scope: CoroutineScope,
        googleIdToken: String,
        name: String,
        onSuccess: () -> Unit
    ) {
        scope.launch {
            try {
                // 1. Send RAW Google Token directly to backend login endpoint
                val authResponse = exchangeToken("users/login/google", googleIdToken)

                if (authResponse != null) {
                    // 2. Keep Firebase instance in sync
                    val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleIdToken, null)
                    firebaseAuth.signInWithCredential(credential).await()

                    sharedPreferencesUtil.saveUser(authResponse.token, name)
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Login failed: Backend rejected token", Toast.LENGTH_LONG).show()
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
                    // 2. Retrieve Firebase ID Token (Backend uses Firebase verifier for email login)
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
                    Toast.makeText(context, "Login failed: Invalid credentials.", Toast.LENGTH_LONG).show()
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
                    // 2. Sign in locally to Firebase
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