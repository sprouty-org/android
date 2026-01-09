package si.uni.fri.sprouty.ui.loading

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import si.uni.fri.sprouty.MainActivity
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.data.network.AuthApiService
import si.uni.fri.sprouty.ui.login.LoginActivity
import si.uni.fri.sprouty.ui.register.RegisterActivity
import si.uni.fri.sprouty.util.auth.JwtUtils
import si.uni.fri.sprouty.util.network.NetworkModule
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil

class LoadingActivity : AppCompatActivity() {

    private lateinit var sharedPreferencesUtil: SharedPreferencesUtil
    private lateinit var jwtUtils: JwtUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Splash Screen before super.onCreate
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        // Keep splash on screen until we decide where to navigate
        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }

        sharedPreferencesUtil = SharedPreferencesUtil(applicationContext)
        val authApiService = NetworkModule.provideRetrofit(applicationContext).create(AuthApiService::class.java)
        jwtUtils = JwtUtils(authApiService, sharedPreferencesUtil)

        lifecycleScope.launch {
            delay(500)
            handleStartupAuth()
            isReady = true
        }
    }

    private suspend fun handleStartupAuth() {
        val savedJwt = sharedPreferencesUtil.getAuthToken()

        // Case A: New user or logged out
        if (savedJwt == null) {
            Log.d("Loading", "No token found, going to Register.")
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
            return
        }

        // Case B: Token exists and is still valid
        if (!jwtUtils.isExpired(savedJwt)) {
            Log.d("Loading", "Token valid, going to Main.")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Case C: Token expired, try refreshing it automatically
        Log.d("Loading", "Token expired, attempting refresh.")
        if (jwtUtils.refreshJwtToken()) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // Case D: Refresh failed, go to Login
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}