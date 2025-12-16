package si.uni.fri.sprouty.ui.loading

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import si.uni.fri.sprouty.MainActivity
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.ui.login.LoginActivity
import si.uni.fri.sprouty.ui.register.RegisterActivity
// Use the new package locations/names you provided
import si.uni.fri.sprouty.util.auth.JwtUtils
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil
import si.uni.fri.sprouty.util.network.NetworkModule
import si.uni.fri.sprouty.data.network.AuthApiService // Assuming the interface is named AuthService


class LoadingActivity : AppCompatActivity() {

    // 1. Declare properties for your new utility classes (dependencies)
    private lateinit var sharedPreferencesUtil: SharedPreferencesUtil
    private lateinit var jwtUtils: JwtUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        // 2. Manually set up the dependency injection chain
        // A. SharedPreferencesUtil needs Context
        sharedPreferencesUtil = SharedPreferencesUtil(applicationContext)

        // B. JwtUtils needs the SharedPreferencesUtil and the AuthService API
        val retrofit = NetworkModule.provideRetrofit(applicationContext)
        val authApiService = retrofit.create(AuthApiService::class.java)

        // C. Initialize JwtUtils
        jwtUtils = JwtUtils(authApiService, sharedPreferencesUtil)

        // Delay for splash screen effect, then check auth status
        Handler(Looper.getMainLooper()).postDelayed({
            lifecycleScope.launch {
                handleStartupAuth()
            }
        }, 1500)
    }

    /**
     * Handles the initial authentication check.
     */
    private suspend fun handleStartupAuth() {
        // 3. Call methods on the INSTANCES, not the class name, and without Context
        val savedJwt = sharedPreferencesUtil.getAuthToken()

        // 1. If JWT missing → go to register
        if (savedJwt == null) {
            Log.d("LoadingActivity", "JWT missing, navigating to Register.")
            goToRegister()
            return
        }

        // 2. If JWT still valid (not expiring soon) → go to main
        if (!jwtUtils.isExpired(savedJwt)) {
            Log.d("LoadingActivity", "JWT still valid, navigating to Main.")
            goToMain()
            return
        }

        // 3. JWT expired or expiring soon → try automatic refresh
        if(jwtUtils.refreshJwtToken()){ // No Context needed here
            Log.d("LoadingActivity", "JWT refreshed successfully, navigating to Main.")
            goToMain()
            return
        }else{
            // 4. Refresh failed → must log in
            Log.d("LoadingActivity", "JWT refresh failed, navigating to Login.")
            goToLogin()
            return
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun goToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
        finish()
    }
}