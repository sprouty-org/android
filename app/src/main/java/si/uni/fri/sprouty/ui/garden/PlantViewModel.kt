package si.uni.fri.sprouty.ui.garden

import android.content.Context
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
import java.io.File

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

    // Inside PlantViewModel class
    fun connectSensorToPlant(plantId: String?, sensorId: String) {
        if (plantId == null) return
        viewModelScope.launch {
            _isIdentifying.value = true // Reuse loading state
            val success = repository.connectSensor(plantId, sensorId)
            _isIdentifying.value = false
            if (success) {
                // Optional: Use a SharedFlow to signal success toast
            }
        }
    }

    /**
     * Triggered after the user takes a photo.
     * Handles image conversion and calling the repository.
     */
    fun identifyAndAddPlant(bitmap: Bitmap, context: Context) {
        viewModelScope.launch {
            _isIdentifying.value = true

            // 1. Save Bitmap to internal storage to get a permanent File Path
            val imageFile = File(context.filesDir, "plant_${System.currentTimeMillis()}.jpg")
            context.openFileOutput(imageFile.name, Context.MODE_PRIVATE).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            val imagePath = imageFile.absolutePath

            // 2. Prepare the Multipart request
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, stream)
            val byteArray = stream.toByteArray()

            val requestFile = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", "photo.jpg", requestFile)

            // 3. Pass the REAL imagePath to the repository
            val result = repository.identifyAndSavePlant(body, imagePath)

            _identificationResult.value = result
            _isIdentifying.value = false
        }
    }

    /**
     * Pull-to-refresh logic.
     */
    fun refreshData() = viewModelScope.launch {
        repository.syncPlantsFromRemote()
    }

    /**
     * Delete plant from local and remote.
     */
    fun deletePlant(plant: Plant) = viewModelScope.launch {
        repository.deletePlant(plant)
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