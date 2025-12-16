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
import si.uni.fri.sprouty.data.repository.PlantRepository // Dependency for logout cleanup
import si.uni.fri.sprouty.data.network.AuthResponse
import si.uni.fri.sprouty.data.network.GoogleLoginRequest
import si.uni.fri.sprouty.data.network.RegisterRequest
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil

/**
 * Utility class for handling Firebase Auth flows and exchanging tokens with the Spring Boot backend.
 *
 * NOTE: Converted to a class to be injectable, requiring dependencies in the constructor.
 */
class FirebaseUtils(
    private val authApiService: AuthApiService,
    private val sharedPreferencesUtil: SharedPreferencesUtil,
    private val plantRepository: PlantRepository? = null // Optional dependency for clearing data
) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val TAG = "FirebaseUtils"

    // --- Core Network Operations (Logic moved from APIHandler.kt) ---

    /**
     * Handles the Google Sign-Up/Registration flow.
     * 1. Uses the Google ID token to authenticate/register the user on the Spring Boot backend.
     * 2. Uses the Google ID token to sign in/link the user with Firebase.
     * 3. Retrieves the new Firebase ID token.
     * 4. Exchanges the Firebase ID token for a JWT with the backend (using the LOGIN endpoint).
     * 5. Saves the JWT locally.
     *
     * NOTE: This assumes the backend's 'register/google' endpoint handles the initial user creation.
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
                // 1. Exchange Token with Backend using a dedicated REGISTER endpoint
                // We use the same 'exchangeToken' utility but hit the 'register/google' path.
                val authResponse = exchangeToken("users/register/google", googleIdToken)

                if (authResponse != null) {
                    // 2. Link/Sign in the user with Firebase
                    val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleIdToken, null)

                    // We use signInWithCredential to authenticate the user in Firebase
                    val authResult = firebaseAuth.signInWithCredential(credential).await()
                    val firebaseUser = authResult.user

                    if (firebaseUser != null) {
                        // 3. Save JWT and succeed
                        sharedPreferencesUtil.saveUser(authResponse.token, name)
                        withContext(Dispatchers.Main) { onSuccess() }
                    } else {
                        // This should ideally not happen if step 2 was successful
                        Toast.makeText(context, "Registration success, but Firebase sign-in failed.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Backend registration failed (e.g., user already exists)
                    Toast.makeText(context, "Google Registration failed (Backend error).", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Google Registration error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Google Sign-Up failed.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun exchangeToken(endpointPath: String, firebaseIdToken: String): AuthResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val request = GoogleLoginRequest(firebaseIdToken)
                val response = authApiService.exchangeToken(endpointPath, request)
                if (response.isSuccessful) {
                    response.body()
                } else {
                    Log.e(TAG, "Token Exchange Failed ($endpointPath): Code ${response.code()} - ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Token Exchange Network Exception ($endpointPath): ${e.message}")
                null
            }
        }
    }

    private suspend fun register(email: String, pass: String, name: String): AuthResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApiService.register(RegisterRequest(email, pass, name))
                if (response.isSuccessful) {
                    response.body()
                } else {
                    Log.e(TAG, "Registration failed: Code ${response.code()} - ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration Network Exception: ${e.message}")
                null
            }
        }
    }

    // --- Authentication Flows ---

    /**
     * Handles traditional Email/Password login.
     * 1. Uses Firebase to authenticate the user.
     * 2. Retrieves the Firebase ID token.
     * 3. Exchanges the Firebase ID token for a JWT with the backend.
     * 4. Saves the JWT locally.
     */
    fun loginWithEmail(
        context: Context,
        coroutineScope: CoroutineScope,
        email: String,
        pass: String,
        onSuccess: () -> Unit
    ) {
        coroutineScope.launch {
            try {
                // 1. Authenticate with Firebase
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    // 2. Retrieve Firebase ID Token
                    val firebaseIdToken = firebaseUser.getIdToken(true).await()?.token
                    if (firebaseIdToken == null) {
                        Toast.makeText(context, "Failed to get Firebase token.", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    // 3. Exchange Token with Backend
                    val authResponse = exchangeToken("users/login/email", firebaseIdToken)
                    if (authResponse != null) {
                        // 4. Save JWT and succeed
                        sharedPreferencesUtil.saveUser(authResponse.token, firebaseUser.displayName ?: email)
                        withContext(Dispatchers.Main) { onSuccess() }
                    } else {
                        // Backend exchange failed
                        Toast.makeText(context, "Login failed. Backend authorization error.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Email/Password login error: ${e.message}")
                // Common error: Firebase authentication failure (wrong credentials)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Login failed: Invalid email or password.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Handles Google Sign-In flow for a logging in (existing) user.
     * 1. Creates a Firebase credential from the Google ID token.
     * 2. Signs in/links the user with Firebase.
     * 3. Retrieves the new Firebase ID token.
     * 4. Exchanges the Firebase ID token for a JWT with the backend (using the LOGIN endpoint).
     * 5. Saves the JWT locally.
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
                // 1. Get Firebase ID token
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user

                if (firebaseUser == null) {
                    Toast.makeText(context, "Firebase sign-in failed.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // 2. Retrieve Firebase ID Token
                val firebaseToken = firebaseUser.getIdToken(true).await()?.token
                if (firebaseToken == null) {
                    Toast.makeText(context, "Failed to retrieve Firebase ID token.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // 3. Exchange with backend
                val authResponse = exchangeToken("users/login/google", firebaseToken)

                if (authResponse != null) {
                    // 4. Save JWT and succeed
                    sharedPreferencesUtil.saveUser(authResponse.token, name)
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    // Backend exchange failed
                    Toast.makeText(context, "Google Login failed. Backend authorization error.", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Google Login error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Google Sign-In failed.", Toast.LENGTH_LONG).show()
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
            val authResponse = register(email, pass, name)
            if (authResponse != null) {
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        sharedPreferencesUtil.saveUser(authResponse.token, name)
                        onSuccess()
                    } else {
                        Log.e(TAG, "Firebase sign-in failed after backend success: ${task.exception?.message}")
                        Toast.makeText(context, "Local sign-in failed. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(context, "Registration failed (Backend error).", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Securely logs out the user by clearing local data and Firebase session.
     */
    fun logout(context: Context, onLogoutSuccess: () -> Unit) {
        Log.i(TAG, "Starting secure user logout...")
        firebaseAuth.signOut()
        sharedPreferencesUtil.clearUser()

        if (plantRepository != null) {
            CoroutineScope(Dispatchers.IO).launch {
                plantRepository.clearLocalData() // Clears Room DB
                Log.d(TAG, "Local Room data cleared.")
                withContext(Dispatchers.Main) { onLogoutSuccess() }
            }
        } else {
            onLogoutSuccess()
        }
    }
}