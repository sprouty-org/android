package si.uni.fri.sprouty.ui.register

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
import si.uni.fri.sprouty.MainActivity
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.data.database.AppDatabase
import si.uni.fri.sprouty.data.network.AuthApiService
import si.uni.fri.sprouty.data.network.PlantApiService
import si.uni.fri.sprouty.data.repository.PlantRepository
import si.uni.fri.sprouty.ui.login.LoginActivity
import si.uni.fri.sprouty.util.auth.FirebaseUtils
import si.uni.fri.sprouty.util.network.NetworkModule
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil

private const val RC_SIGN_IN = 1001

class RegisterActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseUtils: FirebaseUtils

    private lateinit var loadingOverlay: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        loadingOverlay = findViewById(R.id.loadingOverlay)

        // --- DEPENDENCY SETUP ---
        val retrofit = NetworkModule.provideRetrofit(applicationContext)
        val authApiService = retrofit.create(AuthApiService::class.java)
        val plantApiService = retrofit.create(PlantApiService::class.java)

        val db = AppDatabase.getDatabase(applicationContext)
        val plantRepository = PlantRepository(db.plantDao(), plantApiService)
        val sharedPrefs = SharedPreferencesUtil(applicationContext)

        // Instantiate FirebaseUtils with all dependencies
        firebaseUtils = FirebaseUtils(authApiService, sharedPrefs, plantRepository)

        // --- UI BINDING ---
        val emailField = findViewById<EditText>(R.id.inputEmail)
        val passwordField = findViewById<EditText>(R.id.inputPassword)
        val repeatPasswordField = findViewById<EditText>(R.id.inputRepeatPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val btnGoogleRegister = findViewById<ImageButton>(R.id.btnGoogle)
        val btnGoToLogin = findViewById<Button>(R.id.btnGoToLogin)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Listeners
        btnSignUp.setOnClickListener {
            val email = emailField.text.toString().trim()
            val pass = passwordField.text.toString().trim()
            val repeat = repeatPasswordField.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass != repeat) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setLoading(true)

            firebaseUtils.registerUser(
                context = this,
                scope = lifecycleScope,
                email = email,
                pass = pass,
                name = email.substringBefore("@"),
                onSuccess = { goToMain() },
                onFailure = { setLoading(false) }
            )
        }

        btnGoogleRegister.setOnClickListener {
            setLoading(true)
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        btnGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val token = account.idToken
                if (token != null) {
                    firebaseUtils.exchangeGoogleRegisterToken(
                        context = this,
                        scope = lifecycleScope,
                        googleIdToken = token,
                        name = account.displayName ?: "User",
                        onSuccess = { goToMain() },
                        onFailure = { setLoading(false) }
                    )
                }
            } catch (e: ApiException) {
                Log.e("RegisterActivity", "Google Sign-In failed: ${e.message}")
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