package si.uni.fri.sprouty.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.data.database.AppDatabase
import si.uni.fri.sprouty.data.repository.PlantRepository
import si.uni.fri.sprouty.util.network.NetworkModule
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil
import kotlin.random.Random

class MyFcmService : FirebaseMessagingService() {

    // Use a dedicated scope for the service
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Handle DATA payload (Silent Update/Sync)
        // This is what updates your UI automatically
        remoteMessage.data["action"]?.let { action ->
            if (action == "REFRESH_PLANTS") {
                triggerBackgroundSync()
            }
        }

        // 2. Handle VISIBLE notification payload (User Alert)
        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
        }
    }

    private fun triggerBackgroundSync() {
        Log.d("FCM", "Triggering background sync from FCM data message...")

        // Initialize repository on the fly (or get from your Application class)
        val plantDao = AppDatabase.getDatabase(applicationContext).plantDao()
        val apiService = NetworkModule.provideRetrofit(applicationContext).create(PlantApiService::class.java)
        val repository = PlantRepository(plantDao, apiService)

        serviceScope.launch {
            try {
                repository.syncPlantsFromRemote()
                Log.d("FCM", "Background sync successful.")
            } catch (e: Exception) {
                Log.e("FCM", "Background sync failed: ${e.message}")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token generated: $token")
        sendTokenToBackend(token)
    }

    private fun sendTokenToBackend(token: String) {
        val sharedPrefs = SharedPreferencesUtil(applicationContext)
        val jwt = sharedPrefs.getAuthToken()

        if (jwt == null) {
            Log.d("FCM", "No JWT found. Token saved for next login.")
            return
        }

        val api = NetworkModule.provideRetrofit(applicationContext).create(AuthApiService::class.java)

        serviceScope.launch {
            try {
                // Assuming UpdateFcmRequest is your DTO for token updates
                val response = api.updateFcmToken(UpdateFcmRequest(token))
                if (response.isSuccessful) {
                    Log.d("FCM", "Backend updated with new FCM token")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Failed to update backend token: ${e.message}")
            }
        }
    }

    private fun showNotification(title: String?, message: String?) {
        val channelId = "plant_alerts"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Plant Health Alerts",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_logo) // Ensure this icon exists
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }
}