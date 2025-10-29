package si.uni.fri.sprouty.loading

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import si.uni.fri.sprouty.MainActivity
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.login.LoginActivity


class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val token = prefs.getString("auth_token", null)

            if (token != null) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 1500)
    }
}
