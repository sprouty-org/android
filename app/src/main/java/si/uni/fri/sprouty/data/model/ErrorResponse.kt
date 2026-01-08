package si.uni.fri.sprouty.data.model
import com.google.gson.Gson
import retrofit2.Response

data class ErrorResponse(
    val message: String,
    val timestamp: Long,
    val status: Int
)


fun <T> Response<T>.parseError(): ErrorResponse? {
    val errorJson = errorBody()?.string()
    return try {
        Gson().fromJson(errorJson, ErrorResponse::class.java)
    } catch (e: Exception) {
        null
    }
}