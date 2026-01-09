package si.uni.fri.sprouty.util.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPreferencesUtil(context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_PROFILE_NAME = "profile_name"
        private const val KEY_USER_ID = "firebase_uid" // New key for the actual UID
    }

    fun saveUser(jwt: String, name: String, firebaseUid: String) {
        sharedPrefs.edit {
            putString(KEY_AUTH_TOKEN, jwt)
            putString(KEY_PROFILE_NAME, name)
            putString(KEY_USER_ID, firebaseUid) // Save the alphanumeric UID here
        }
    }

    fun saveToken(jwt: String) {
        sharedPrefs.edit { putString(KEY_AUTH_TOKEN, jwt) }
    }

    fun getAuthToken(): String? = sharedPrefs.getString(KEY_AUTH_TOKEN, null)

    fun getProfileName(): String? = sharedPrefs.getString(KEY_PROFILE_NAME, null)

    fun getUserId(): String? = sharedPrefs.getString(KEY_USER_ID, null)

    fun clearUser() {
        sharedPrefs.edit { clear() }
    }
}