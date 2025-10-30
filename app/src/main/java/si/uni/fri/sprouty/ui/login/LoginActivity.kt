package si.uni.fri.sprouty.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import si.uni.fri.sprouty.MainActivity
import si.uni.fri.sprouty.R

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1. Configure sign-in options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // from strings.xml
            .requestEmail()
            .build()

        // 2. Build client
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 3. Setup button click
        findViewById<Button>(R.id.btnOAuth).setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken

                if (idToken != null) {
                    saveUserAndContinue(account.displayName ?: "Unknown", idToken)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserAndContinue(name: String, token: String) {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("auth_token", token)
            .putString("profile_name", name)
            .apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
