package si.uni.fri.sprouty.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import si.uni.fri.sprouty.util.auth.JwtUtils
import si.uni.fri.sprouty.util.storage.SharedPreferencesUtil

class AuthInterceptor(
    private val context: Context,
) : Interceptor {

    private val TAG = "AuthInterceptor"
    private val BASE_URL = "http://sprouty.duckdns.org/"

    private val sharedPrefsUtil by lazy { SharedPreferencesUtil(context.applicationContext) }

    private val baseRetrofit by lazy {
        OkHttpClient.Builder().build()
    }

    private val baseRetrofitService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(baseRetrofit)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    private val jwtUtils by lazy { JwtUtils(baseRetrofitService, sharedPrefsUtil) }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        if (path.contains("/users/register") || path.contains("/users/login")) {
            return chain.proceed(originalRequest)
        }

        var jwt = sharedPrefsUtil.getAuthToken()
        val userId = sharedPrefsUtil.getUserId()

        val requestWithAuth = originalRequest.newBuilder()
            .apply {
                if (!jwt.isNullOrEmpty()) {
                    header("Authorization", "Bearer $jwt")
                }
                if (!userId.isNullOrEmpty()) {
                    val sanitizedId = userId.filter { it.code <= 127 }
                    header("X-User-Id", sanitizedId)
                }
            }
            .build()

        val response = chain.proceed(requestWithAuth)

        if (response.code == 401) {
            response.close()
            Log.d(TAG, "JWT expired or invalid → attempting refresh")

            val tokenRefreshSuccess = runBlocking {
                jwtUtils.refreshJwtToken()
            }

            if (tokenRefreshSuccess) {
                jwt = sharedPrefsUtil.getAuthToken()
                val newUserId = sharedPrefsUtil.getUserId()

                Log.d(TAG, "JWT refreshed successfully. Retrying request.")
                val sanitizedId = newUserId?.filter { it.code <= 127 }

                val retryRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $jwt")
                    .header("X-User-Id", sanitizedId ?: "")
                    .build()

                return chain.proceed(retryRequest)
            } else {
                Log.e(TAG, "Refresh failed. User must log in again.")
                sharedPrefsUtil.clearUser()
            }
        }
        return response
    }
}