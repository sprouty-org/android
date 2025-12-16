package si.uni.fri.sprouty.ui.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import si.uni.fri.sprouty.data.repository.PlantRepository
import si.uni.fri.sprouty.data.model.Plant

/**
 * The ViewModel for the Garden screen (MainActivity).
 * It fetches the list of plants and exposes operations for the UI.
 */
class PlantViewModel(private val repository: PlantRepository) : ViewModel() {

    // --- Data Exposed to UI ---

    /**
     * StateFlow holding the list of plants.
     * The UI (MainActivity) will observe this Flow.
     * The initial value is an empty list, and data is updated by the repository.
     */
    val plantList: StateFlow<List<Plant>> =
        repository.getAllPlants() // Get the Flow of data from the Room database
            .stateIn(
                scope = viewModelScope,
                // Data starts sharing immediately and is kept active while at least one observer is present
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // --- UI Actions ---

    /**
     * Called when the user presses the FAB to add a new plant.
     */
    fun addNewPlant(newPlant: Plant) = viewModelScope.launch {
        repository.saveNewPlant(newPlant)
    }

    /**
     * Called when the user deletes a plant (e.g., swipe action).
     */
    fun deletePlant(plant: Plant) = viewModelScope.launch {
        repository.deletePlant(plant)
    }

    /**
     * Called when the user manually triggers a data refresh (e.g., pull-to-refresh).
     */
    fun refreshData() = viewModelScope.launch {
        repository.syncPlantsFromRemote()
    }

    /**
     * Called during secure logout.
     */
    fun clearLocalCache() = viewModelScope.launch {
        repository.clearLocalData()
    }
}

// --- ViewModel Factory (Required boilerplate for dependency injection) ---

/**
 * Factory class to instantiate the PlantViewModel with the correct repository dependency.
 */
class PlantViewModelFactory(private val repository: PlantRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlantViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}