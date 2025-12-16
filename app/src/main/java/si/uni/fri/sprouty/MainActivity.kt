package si.uni.fri.sprouty

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import si.uni.fri.sprouty.data.database.AppDatabase
import si.uni.fri.sprouty.data.repository.PlantRepository
import si.uni.fri.sprouty.ui.garden.PlantViewModel
import si.uni.fri.sprouty.ui.garden.PlantViewModelFactory
import si.uni.fri.sprouty.ui.settings.SettingsActivity

// ADDED IMPORTS for Network/API
import si.uni.fri.sprouty.util.network.NetworkModule
import si.uni.fri.sprouty.data.network.PlantApiService // Assuming this is the interface location

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: PlantViewModel
    private lateinit var recyclerPlants: RecyclerView
    private lateinit var fabAddPlant: FloatingActionButton
    // TODO: Define PlantAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupDependencies() // NEW: Set up Room, Repository, and ViewModel
        setupIconNavigation()
        setupGardenView()

        // NEW: Start observing the plant data
        observePlantData()

        // Initial sync of data when the main garden loads
        viewModel.refreshData()
    }

    private fun setupDependencies() {
        // 1. Get DAO and setup API Service
        val plantDao = AppDatabase.getDatabase(applicationContext).plantDao()

        // FIX: Use NetworkModule to provide the API Service, matching how it was used in LoginActivity.
        val retrofit = NetworkModule.provideRetrofit(applicationContext)
        val apiService = retrofit.create(PlantApiService::class.java)

        // 2. Create Repository
        val repository = PlantRepository(plantDao, apiService)

        // 3. Initialize ViewModel using the Factory
        viewModel = ViewModelProvider(
            this,
            PlantViewModelFactory(repository)
        )[PlantViewModel::class.java]
    }

    private fun setupGardenView() {
        recyclerPlants = findViewById(R.id.recyclerPlants)
        fabAddPlant = findViewById(R.id.fabAddPlant)

        // Setup RecyclerView
        recyclerPlants.layoutManager = GridLayoutManager(this, 2)
        // TODO: recyclerPlants.adapter = PlantAdapter(...)

        // Setup FAB click listener
        fabAddPlant.setOnClickListener {
            // TODO: Launch the Add New Plant screen/dialog
        }
    }

    private fun observePlantData() {
        // Collect the StateFlow data from the ViewModel
        lifecycleScope.launch {
            viewModel.plantList.collect { plants ->
                // This block runs whenever the data in the Room database changes!
                // TODO: Update your RecyclerView adapter here
                // (recyclerPlants.adapter as PlantAdapter).submitList(plants)
                Log.d("MainActivity", "Received ${plants.size} plants from ViewModel.")
            }
        }
    }

    // --- Icon Navigation (Same as before) ---
    private fun setupIconNavigation() {
        // ... (Code for Shop and Settings icon clicks remains here)
        findViewById<ImageView>(R.id.icon_settings).setOnClickListener {
            // Start the Settings Activity
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        // ...
    }
}