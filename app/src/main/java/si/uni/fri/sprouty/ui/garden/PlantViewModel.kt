package si.uni.fri.sprouty.ui.garden

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
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

    // --- State Management ---

    val plantList: StateFlow<List<Plant>> = repository.getAllPlants()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isIdentifying = MutableStateFlow(false)
    val isIdentifying = _isIdentifying.asStateFlow()

    private val _identificationResult = MutableStateFlow<PlantIdentificationResponse?>(null)
    val identificationResult = _identificationResult.asStateFlow()

    // --- Actions ---

    fun renamePlant(plantId: String?, newName: String) {
        viewModelScope.launch {
            _isIdentifying.value = true
            try {
                // Assuming repository has an updatePlantName method
                repository.updatePlantName(plantId, newName)
                // Refresh to sync local DB with remote change
                repository.syncPlantsFromRemote()
            } finally {
                _isIdentifying.value = false
            }
        }
    }

    fun disconnectSensorFromPlant(plantId: String?) {
        viewModelScope.launch {
            _isIdentifying.value = true
            try {
                // Assuming repository has a disconnectSensor method
                repository.disconnectSensor(plantId)
                repository.syncPlantsFromRemote()
            } finally {
                _isIdentifying.value = false
            }
        }
    }

    // Updated to match your MainActivity's call (using String ID)
    fun deletePlant(plantId: String?) {
        viewModelScope.launch {
            _isIdentifying.value = true
            try {
                // Find the plant in current list to pass to repository delete
                val plantToDelete = plantList.value.find { it.firebaseId == plantId }
                plantToDelete?.let {
                    repository.deletePlant(it)
                }
            } finally {
                _isIdentifying.value = false
            }
        }
    }

    fun connectSensorToPlant(plantId: String?, sensorId: String) {
        if (plantId == null) return
        viewModelScope.launch {
            _isIdentifying.value = true
            val success = repository.connectSensor(plantId, sensorId)
            if (success) {
                repository.syncPlantsFromRemote()
            }
            _isIdentifying.value = false
        }
    }

    fun identifyAndAddPlant(bitmap: Bitmap, context: Context) {
        viewModelScope.launch {
            _isIdentifying.value = true
            try {
                val imageFile = File(context.filesDir, "plant_${System.currentTimeMillis()}.jpg")
                context.openFileOutput(imageFile.name, Context.MODE_PRIVATE).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
                val imagePath = imageFile.absolutePath

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 30, stream)
                val byteArray = stream.toByteArray()

                val requestFile = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("image", "photo.jpg", requestFile)

                val result = repository.identifyAndSavePlant(body, imagePath)
                _identificationResult.value = result
            } finally {
                _isIdentifying.value = false
            }
        }
    }

    fun refreshData() = viewModelScope.launch {
        repository.syncPlantsFromRemote()
    }

    fun resetIdentificationResult() {
        _identificationResult.value = null
    }
}

class PlantViewModelFactory(private val repository: PlantRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlantViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}