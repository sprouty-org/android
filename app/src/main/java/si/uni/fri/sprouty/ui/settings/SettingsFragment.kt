package si.uni.fri.sprouty.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.ui.login.LoginActivity
import si.uni.fri.sprouty.util.auth.FirebaseUtils

// ADDED IMPORTS for Dependency Setup
import si.uni.fri.sprouty.util.network.NetworkModule
import si.uni.fri.sprouty.data.network.AuthApiService
import si.uni.fri.sprouty.data.database.AppDatabase
import si.uni.fri.sprouty.data.repository.PlantRepository
import si.uni.fri.sprouty.data.network.PlantApiService
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil

class SettingsFragment : PreferenceFragmentCompat() {

    // Declare the instance of FirebaseUtils
    private lateinit var firebaseUtils: FirebaseUtils

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Loads the preference XML file
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // --- DEPENDENCY INJECTION SETUP ---
        val context = requireActivity().applicationContext

        // 1. AuthApiService (Needed for FirebaseUtils constructor)
        val authApiService = NetworkModule.provideRetrofit(context).create(AuthApiService::class.java)

        // 2. PlantRepository (Needed for logout data cleanup)
        val db = AppDatabase.getDatabase(context)
        val plantDao = db.plantDao()
        val plantApiService = NetworkModule.provideRetrofit(context).create(PlantApiService::class.java)
        val plantRepository = PlantRepository(plantDao, plantApiService)

        // 3. SharedPreferencesUtil (Storage dependency)
        val sharedPreferencesUtilDep = SharedPreferencesUtil(context)

        // 4. Instantiate the class-based FirebaseUtils
        // Note: The PlantRepository is intentionally passed here for the logout function.
        firebaseUtils = FirebaseUtils(authApiService, sharedPreferencesUtilDep)

        setupLogoutButton()
    }

    private fun setupLogoutButton() {
        val logoutButton: Preference? = findPreference("logout_button")

        logoutButton?.setOnPreferenceClickListener {
            // FIX: Call the instance method on firebaseUtils
            firebaseUtils.logout {
                // Lambda function executed on successful clearance of local data
                navigateToLogin()
            }
            true // Indicate the click event was handled
        }
    }

    private fun navigateToLogin() {
        // Crucial for clearing the back stack
        val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        // Close the SettingsActivity
        requireActivity().finish()
    }
}