package si.uni.fri.sprouty.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import si.uni.fri.sprouty.MainActivity
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.data.network.AuthApiService
import si.uni.fri.sprouty.ui.register.RegisterActivity
import si.uni.fri.sprouty.util.auth.FirebaseUtils
import si.uni.fri.sprouty.util.network.NetworkModule
import si.uni.fri.sprouty.data.database.AppDatabase
import si.uni.fri.sprouty.data.repository.PlantRepository
import si.uni.fri.sprouty.data.network.PlantApiService
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil // Assuming this is the correct import

private const val RC_SIGN_IN = 1001

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUtils: FirebaseUtils // Dependency instance

    private lateinit var loadingOverlay: ConstraintLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loadingOverlay = findViewById(R.id.loadingOverlay) // Bind overlay

        // INITIALIZATION
        firebaseAuth = FirebaseAuth.getInstance()

        // --- DEPENDENCY INJECTION SETUP ---

        // 1. AuthApiService (Network dependency)
        val authApiService = NetworkModule.provideRetrofit(applicationContext).create(AuthApiService::class.java)

        // 2. PlantRepository (Database dependency for logout cleanup)
        val db = AppDatabase.getDatabase(applicationContext)
        val plantDao = db.plantDao()
        val plantApiService = NetworkModule.provideRetrofit(applicationContext).create(PlantApiService::class.java)
        val plantRepository = PlantRepository(plantDao, plantApiService)

        // 3. SharedPreferencesUtil (Storage dependency)
        // FIX: Instantiate the class by passing context, assuming the class constructor requires it.
        val sharedPreferencesUtilDep = SharedPreferencesUtil(applicationContext)

        // 4. Instantiate the class-based FirebaseUtils
        firebaseUtils = FirebaseUtils(authApiService, sharedPreferencesUtilDep, plantRepository)

        // --- UI Setup ---

        val emailField = findViewById<EditText>(R.id.inputEmail)
        val passwordField = findViewById<EditText>(R.id.inputPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        val btnGoogleLogin = findViewById<ImageButton>(R.id.btnGoogle)
        val btnSignup = findViewById<Button>(R.id.btnGoToSignup)


        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Set up Listeners
        btnLogin.setOnClickListener {
            val email = emailField.text.toString().trim()
            val pass = passwordField.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true) // Start loading
            // FIX: Call instance method on firebaseUtils
            // NOTE: This assumes you ADD the loginWithEmail method to FirebaseUtils.
            firebaseUtils.loginWithEmail(
                context = this,
                coroutineScope = lifecycleScope,
                email = email,
                pass = pass,
                onSuccess = { goToMain() },
                onFailure = { setLoading(false) }
            )
        }

        btnGoogleLogin.setOnClickListener {
            setLoading(true)
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        btnSignup.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
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
                    // FIX: Call instance method on firebaseUtils
                    // NOTE: This assumes you ADD the exchangeGoogleLoginToken method to FirebaseUtils.
                    firebaseUtils.exchangeGoogleLoginToken(
                        this,
                        lifecycleScope,
                        googleIdToken,
                        name,
                        onSuccess = { goToMain() },
                        onFailure = { setLoading(false) }
                    )
                }
            } catch (e: ApiException) {
                Log.e("LoginActivity", "Google Sign-In failed", e)
                Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}