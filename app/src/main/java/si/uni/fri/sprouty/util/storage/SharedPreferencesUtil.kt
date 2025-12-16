package si.uni.fri.sprouty.util.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Utility class for managing JWT and user profile in local SharedPreferences.
 *
 * NOTE: This class has been converted to a standard class to be injectable,
 * requiring an Application Context in the constructor for safe access.
 */
class SharedPreferencesUtil(context: Context) { // Converted from object to class

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_PROFILE_NAME = "profile_name"
    }

    /**
     * Saves both the JWT token and the user's display name.
     */
    fun saveUser(jwt: String, name: String) {
        sharedPrefs.edit {
            putString(KEY_AUTH_TOKEN, jwt)
            putString(KEY_PROFILE_NAME, name)
        }
    }

    /**
     * Saves only the JWT token (used during token refresh).
     */
    fun saveToken(jwt: String) {
        sharedPrefs.edit { putString(KEY_AUTH_TOKEN, jwt) }
    }

    /**
     * Retrieves the saved JWT token.
     */
    fun getAuthToken(): String? {
        return sharedPrefs.getString(KEY_AUTH_TOKEN, null)
    }

    /**
     * Clears all user data (JWT and profile name).
     */
    fun clearUser() {
        sharedPrefs.edit { clear() }
    }

    fun getSharedPreferences(): SharedPreferences {
        return sharedPrefs
    }
}