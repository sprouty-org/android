package si.uni.fri.sprouty.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import si.uni.fri.sprouty.R


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // You might use a simple layout file, or just define the content
        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }

        // Optional: Add a toolbar/action bar here if your theme doesn't handle it.
    }
}