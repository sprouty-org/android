package si.uni.fri.sprouty.ui.garden

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import si.uni.fri.sprouty.data.repository.PlantRepository
import si.uni.fri.sprouty.data.model.Plant
import si.uni.fri.sprouty.data.model.PlantIdentificationResponse
import java.io.ByteArrayOutputStream

class PlantViewModel(private val repository: PlantRepository) : ViewModel() {

    // --- 1. State Management ---

    // List of plants from Room (Automatic updates)
    val plantList: StateFlow<List<Plant>> = repository.getAllPlants()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI State for the Identification process
    private val _isIdentifying = MutableStateFlow(false)
    val isIdentifying = _isIdentifying.asStateFlow()

    private val _identificationResult = MutableStateFlow<PlantIdentificationResponse?>(null)
    val identificationResult = _identificationResult.asStateFlow()

    // --- 2. Actions ---

    /**
     * Triggered after the user takes a photo.
     * Handles image conversion and calling the repository.
     */
    fun identifyAndAddPlant(userId: String, bitmap: Bitmap) {
        viewModelScope.launch {
            _isIdentifying.value = true

            // Convert Bitmap to MultipartBody.Part
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val byteArray = stream.toByteArray()
            val requestFile = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", "upload.jpg", requestFile)

            // Call Repository (which saves to Room automatically on success)
            val result = repository.identifyAndSavePlant(userId, body)

            _identificationResult.value = result
            _isIdentifying.value = false
        }
    }

    /**
     * Pull-to-refresh logic.
     */
    fun refreshData(userId: String) = viewModelScope.launch {
        repository.syncPlantsFromRemote(userId)
    }

    /**
     * Delete plant from local and remote.
     */
    fun deletePlant(userId: String, plant: Plant) = viewModelScope.launch {
        repository.deletePlant(userId, plant)
    }

    fun clearLocalCache() = viewModelScope.launch {
        repository.clearLocalData()
    }

    // Reset result after showing the success dialog
    fun resetIdentificationResult() {
        _identificationResult.value = null
    }
}

class PlantViewModelFactory(private val repository: PlantRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlantViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}