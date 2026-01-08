package si.uni.fri.sprouty.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.launch
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.data.network.AuthApiService
import si.uni.fri.sprouty.ui.login.LoginActivity
import si.uni.fri.sprouty.ui.register.RegisterActivity
import si.uni.fri.sprouty.util.auth.FirebaseUtils
import si.uni.fri.sprouty.util.network.NetworkModule
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var firebaseUtils: FirebaseUtils

    private var progressDialog: AlertDialog? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val context = requireActivity().applicationContext
        val authApiService = NetworkModule.provideRetrofit(context).create(AuthApiService::class.java)
        val sharedPreferencesUtilDep = SharedPreferencesUtil(context)

        // Assuming your FirebaseUtils handles the cleanup logic
        firebaseUtils = FirebaseUtils(authApiService, sharedPreferencesUtilDep)

        setupLogoutButton()
        setupDeleteAccountButton()
    }

    private fun setupLogoutButton() {
        findPreference<Preference>("logout_button")?.setOnPreferenceClickListener {
            // 1. Tell the UI to clear immediately (Optional but helpful)
            // plantViewModel.clearUiList()

            lifecycleScope.launch {
                // 2. Perform the heavy cleaning
                firebaseUtils.logout(this) {
                    // 3. Navigate ONLY after DB is confirmed empty
                    navigateToLogin()
                }
            }
            true
        }
    }

    private fun setupDeleteAccountButton() {
        findPreference<Preference>("delete_account_button")?.setOnPreferenceClickListener {
            showDeleteConfirmation()
            true
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account?")
            .setMessage("This action is permanent. All your plants and sensor data will be deleted forever.")
            .setPositiveButton("Delete Everything") { _, _ ->
                showLoading()

                // Pre-emptively clear the image cache so photos don't "ghost"
                coil.Coil.imageLoader(requireContext()).memoryCache?.clear()

                // 2. Call the deletion logic
                firebaseUtils.deleteUser(requireContext(), viewLifecycleOwner.lifecycleScope) {
                    hideLoading()
                    navigateToRegister()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLoading() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(R.layout.dialog_loading)
        builder.setCancelable(false)
        progressDialog = builder.create()
        progressDialog?.show()
    }

    private fun hideLoading() {
        progressDialog?.dismiss()
    }

    private fun navigateToRegister() {
        val intent = Intent(requireActivity(), RegisterActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
}
