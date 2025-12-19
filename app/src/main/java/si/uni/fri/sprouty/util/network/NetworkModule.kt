package si.uni.fri.sprouty.util.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import si.uni.fri.sprouty.data.network.AuthInterceptor
import java.util.concurrent.TimeUnit

/**
 * Singleton object to provide a configured Retrofit instance.
 * It sets the base URL, adds a logging interceptor for debugging,
 * and includes the custom AuthInterceptor for JWT handling.
 */
object NetworkModule {

    // IMPORTANT: Use the correct URL for your local network/Docker setup.
    // '10.0.2.2' is the standard way for an Android Emulator to access the host machine's localhost.
    private const val BASE_URL = "http://192.168.1.15:8080/"

    /**
     * Creates and configures the Retrofit instance.
     * @param context Application context, used for safely accessing SharedPreferences
     * and initializing utilities inside the Interceptor.
     */
    fun provideRetrofit(context: Context): Retrofit {
        // 1. Logging Interceptor (for debugging API requests/responses)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Change from BODY to HEADERS to avoid the "closed" crash
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        // 2. OkHttpClient with the Interceptors
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // Log request/response
            // Add the custom interceptor to attach JWT and handle 401 refresh
            .addInterceptor(AuthInterceptor(context.applicationContext)) // Use application context for safety
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        // 3. Build Retrofit
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}