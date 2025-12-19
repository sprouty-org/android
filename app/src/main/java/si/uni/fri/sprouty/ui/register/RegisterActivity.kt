package si.uni.fri.sprouty.ui.register

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import si.uni.fri.sprouty.MainActivity
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.ui.login.LoginActivity
import si.uni.fri.sprouty.util.auth.FirebaseUtils
import si.uni.fri.sprouty.util.network.NetworkModule
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil
import si.uni.fri.sprouty.data.network.AuthApiService
import si.uni.fri.sprouty.data.database.AppDatabase
import si.uni.fri.sprouty.data.repository.PlantRepository
import si.uni.fri.sprouty.data.network.PlantApiService

private const val RC_SIGN_IN = 1001

class RegisterActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUtils: FirebaseUtils // Dependency instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // INITIALIZATION
        firebaseAuth = FirebaseAuth.getInstance()

        // --- DEPENDENCY INJECTION SETUP (Same as LoginActivity) ---

        // 1. AuthApiService (Network dependency)
        val authApiService = NetworkModule.provideRetrofit(applicationContext).create(AuthApiService::class.java)

        // 2. PlantRepository (Database dependency for logout cleanup)
        val db = AppDatabase.getDatabase(applicationContext)
        val plantDao = db.plantDao()
        val plantApiService = NetworkModule.provideRetrofit(applicationContext).create(PlantApiService::class.java)
        val plantRepository = PlantRepository(plantDao, plantApiService)

        // 3. SharedPreferencesUtil (Storage dependency)
        // Instantiate the class by passing context, matching the fix in LoginActivity
        val sharedPreferencesUtilDep = SharedPreferencesUtil(applicationContext)

        // 4. Instantiate the class-based FirebaseUtils
        firebaseUtils = FirebaseUtils(authApiService, sharedPreferencesUtilDep, plantRepository)

        // --- UI Setup ---

        val emailField = findViewById<EditText>(R.id.inputEmail)
        val passwordField = findViewById<EditText>(R.id.inputPassword)
        val repeatPasswordField = findViewById<EditText>(R.id.inputRepeatPassword)

        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val btnGoogleRegister = findViewById<ImageButton>(R.id.btnGoogle)
        val btnGoToLogin = findViewById<Button>(R.id.btnGoToLogin)

        // Configure Google Sign-In (Unchanged)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Google Sign Up Listener
        btnGoogleRegister.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }

        // Email/Password Sign Up (Registration) Listener
        btnSignUp.setOnClickListener {
            val email = emailField.text.toString().trim()
            val pass = passwordField.text.toString().trim()
            val repeatPass = repeatPasswordField.text.toString().trim()

            // 1. Validation
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != repeatPass) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(pass.length < 6){
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // )

            // 2. Extract a display name (You might want a dedicated name input field)
            val displayName = email.substringBefore("@")

            // 3. Call Spring-heavy Register flow
            // FIX: Call instance method on firebaseUtils
            firebaseUtils.registerUser(
                context = this,
                scope = lifecycleScope,
                email = email,
                pass = pass,
                name = displayName,
                onSuccess = { goToMain() }
            )
        }

        // NEW: Go to Login Screen Listener
        btnGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Finish registration activity so user can't press back to it
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val googleIdToken = account.idToken
                val name = account.displayName ?: "User"

                if (googleIdToken != null) {
                    // IMPORTANT CHANGE: Call the new dedicated function
                    // FIX: Call instance method on firebaseUtils
                    firebaseUtils.exchangeGoogleRegisterToken(
                        this,
                        lifecycleScope,
                        googleIdToken,
                        name,
                        onSuccess = { goToMain() }
                    )
                }
            } catch (e: ApiException) {
                Log.e("RegisterActivity", "Google Sign-In failed", e)
                Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}