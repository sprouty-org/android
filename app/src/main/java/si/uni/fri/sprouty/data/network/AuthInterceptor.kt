package si.uni.fri.sprouty.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import si.uni.fri.sprouty.util.auth.JwtUtils // Corrected path/class name
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil // Corrected class name

/**
 * OkHttp Interceptor responsible for:
 * 1. Attaching the JWT to the Authorization header of every outgoing request.
 * 2. Catching a 401 (Unauthorized) response and attempting to refresh the JWT token.
 *
 * NOTE: This Interceptor is self-sufficient. It manually instantiates its dependencies
 * using the Context, which is necessary when combining class-based utilities with an
 * OkHttp interceptor chain without a full DI framework.
 */
class AuthInterceptor(
    private val context: Context,
) : Interceptor {

    private val TAG = "AuthInterceptor"
    private val BASE_URL = "http://192.168.1.15:8080/"

    // Manually instantiate the dependencies needed inside the interceptor using lazy
    private val sharedPrefsUtil by lazy { SharedPreferencesUtil(context.applicationContext) }

    // CRITICAL: We create a separate Retrofit instance *without* this interceptor
    // to avoid a recursive loop when JwtUtils.refreshJwtToken() makes its own API call.
    private val baseRetrofit by lazy {
        OkHttpClient.Builder()
            .build() // No interceptors
    }

    private val baseRetrofitService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // Must match NetworkModule's BASE_URL
            .client(baseRetrofit)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java) // Uses the correct interface name
    }


    // The JwtUtils class instance that has the correct dependencies
    private val jwtUtils by lazy { JwtUtils(baseRetrofitService, sharedPrefsUtil) }


    override fun intercept(chain: Interceptor.Chain): Response {
        // Use the INSTANCE method (no Context needed now that SharedPreferencesUtil is a class)
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // 1. SKIP logic for login/register endpoints
        if (path.contains("/users/register") || path.contains("/users/login")) {
            return chain.proceed(originalRequest)
        }

        var jwt = sharedPrefsUtil.getAuthToken()

        // 2. Attach JWT only for other endpoints (like /plants/identify)
        val requestWithAuth = originalRequest.newBuilder()
            .apply {
                if (jwt != null) {
                    header("Authorization", "Bearer $jwt")
                }
            }
            .build()

        val response = chain.proceed(requestWithAuth)

        // 2) Handle Token Expiration (401 Unauthorized)
        if (response.code == 401) {
            response.close() // Close the failed response before retrying
            Log.d(TAG, "JWT expired or invalid → attempting refresh")

            // We use runBlocking because the interceptor MUST return a Response synchronously
            val tokenRefreshSuccess = runBlocking {
                // Call the instance method on the dependency (no Context needed)
                jwtUtils.refreshJwtToken()
            }

            if (tokenRefreshSuccess) {
                // Get the newly saved token
                jwt = sharedPrefsUtil.getAuthToken()
                Log.d(TAG, "JWT refreshed successfully. Retrying request.")

                // Create and proceed with the retry request
                val retryRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $jwt")
                    .build()

                return chain.proceed(retryRequest)
            } else {
                Log.e(TAG, "Refresh failed. User must log in again.")
                // Clear the local token
                sharedPrefsUtil.clearUser()
            }
        }
        return response
    }
}