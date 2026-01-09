package si.uni.fri.sprouty.ui.garden

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val TAG = "PlantViewModel"

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

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()

    // --- Actions ---

    fun renamePlant(plantId: String?, newName: String) {
        viewModelScope.launch {
            _isIdentifying.value = true
            val result = repository.updatePlantName(plantId, newName)
            result.onSuccess {
                repository.syncPlantsFromRemote()
            }.onFailure { error ->
                _errorEvents.emit(error.message ?: "Rename failed")
            }
            _isIdentifying.value = false
        }
    }

    fun disconnectSensorFromPlant(plantId: String?) {
        viewModelScope.launch {
            _isIdentifying.value = true
            val result = repository.disconnectSensor(plantId)
            result.onSuccess {
                repository.syncPlantsFromRemote()
            }.onFailure { error ->
                _errorEvents.emit(error.message ?: "Disconnect failed")
            }
            _isIdentifying.value = false
        }
    }

    fun deletePlant(plantId: String?) {
        viewModelScope.launch {
            _isIdentifying.value = true
            val plantToDelete = plantList.value.find { it.firebaseId == plantId }
            plantToDelete?.let {
                val result = repository.deletePlant(it)
                result.onFailure { error ->
                    _errorEvents.emit(error.message ?: "Delete failed")
                }
            }
            _isIdentifying.value = false
        }
    }

    fun connectSensorToPlant(plantId: String?, sensorId: String) {
        if (plantId == null) return
        viewModelScope.launch {
            _isIdentifying.value = true
            val result = repository.connectSensor(plantId, sensorId)
            result.onSuccess {
                repository.syncPlantsFromRemote()
            }.onFailure { error ->
                _errorEvents.emit(error.message ?: "Sensor connection failed")
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
                val body = MultipartBody.Part.createFormData(
                    "image",
                    "photo.jpg",
                    stream.toByteArray().toRequestBody("image/jpeg".toMediaTypeOrNull())
                )

                val result = repository.identifyAndSavePlant(body, imagePath)

                result.onSuccess { data ->
                    _identificationResult.value = data
                }.onFailure { error ->
                    _errorEvents.emit(error.message ?: "Identification failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Image prep error: ${e.message}")
                _errorEvents.emit("Failed to process image")
            } finally {
                _isIdentifying.value = false
            }
        }
    }

    fun refreshData() = viewModelScope.launch {
        val result = repository.syncPlantsFromRemote()
        result.onFailure { error ->
            _errorEvents.emit(error.message ?: "Sync failed")
        }
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