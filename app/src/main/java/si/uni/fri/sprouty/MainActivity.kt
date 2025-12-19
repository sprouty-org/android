package si.uni.fri.sprouty

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import si.uni.fri.sprouty.data.database.AppDatabase
import si.uni.fri.sprouty.data.network.PlantApiService
import si.uni.fri.sprouty.data.repository.PlantRepository
import si.uni.fri.sprouty.ui.garden.PlantDetailActivity
import si.uni.fri.sprouty.ui.garden.PlantViewModel
import si.uni.fri.sprouty.ui.garden.PlantViewModelFactory
import si.uni.fri.sprouty.ui.settings.SettingsActivity
import si.uni.fri.sprouty.util.adapters.PlantAdapter
import si.uni.fri.sprouty.util.network.NetworkModule

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: PlantViewModel
    private lateinit var recyclerPlants: RecyclerView
    private lateinit var fabAddPlant: FloatingActionButton
    private lateinit var loadingOverlay: FrameLayout

    private lateinit var plantAdapter: PlantAdapter

    // TODO: Get this dynamically from your SharedPreferences/SessionManager after login
    private val currentUserId = "user_test_123"

    // --- 1. Image Pickers ---

    // Launcher for selecting from Gallery
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageUri(it) }
    }

    // Launcher for taking a new photo
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let { viewModel.identifyAndAddPlant(currentUserId, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupDependencies()
        setupUI()
        observeViewModel()

        // Initial sync: now requires userId
        viewModel.refreshData(currentUserId)
    }

    private fun setupDependencies() {
        val plantDao = AppDatabase.getDatabase(applicationContext).plantDao()
        val retrofit = NetworkModule.provideRetrofit(applicationContext)
        val apiService = retrofit.create(PlantApiService::class.java)

        val repository = PlantRepository(plantDao, apiService)

        viewModel = ViewModelProvider(
            this,
            PlantViewModelFactory(repository)
        )[PlantViewModel::class.java]
    }

    private fun setupUI() {
        recyclerPlants = findViewById(R.id.recyclerPlants)
        fabAddPlant = findViewById(R.id.fabAddPlant)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        recyclerPlants.layoutManager = GridLayoutManager(this, 2)
        // TODO: recyclerPlants.adapter = PlantAdapter(...)

        fabAddPlant.setOnClickListener {
            showImageSourceDialog()
        }

        findViewById<ImageView>(R.id.icon_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Add a New Plant")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun handleImageUri(uri: Uri) {
        try {
            val bitmap =
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            viewModel.identifyAndAddPlant(currentUserId, bitmap)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error decoding gallery image", e)
        }
    }

    private fun setupGardenView() {
        recyclerPlants = findViewById(R.id.recyclerPlants)
        fabAddPlant = findViewById(R.id.fabAddPlant)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        // Set to LinearLayoutManager for vertical list (one per row)
        recyclerPlants.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        plantAdapter = PlantAdapter { plant ->
            val intent = Intent(this, PlantDetailActivity::class.java).apply {
                putExtra("FIREBASE_ID", plant.firebaseId)
                putExtra("SPECIES_NAME", plant.speciesName)
                putExtra("CUSTOM_NAME", plant.customName)
            }
            startActivity(intent)
        }
        recyclerPlants.adapter = plantAdapter

        fabAddPlant.setOnClickListener { showImageSourceDialog() }
    }

    private fun observePlantData() {
        lifecycleScope.launch {
            viewModel.plantList.collect { plants ->
                plantAdapter.submitList(plants) // DIFFUTIL handles the UI refresh automatically
            }
        }
    }

    private fun observeViewModel() {
        // Observe the list of plants (Room)
        lifecycleScope.launch {
            viewModel.plantList.collect { plants ->
                Log.d("MainActivity", "Garden updated: ${plants.size} plants.")
                // (recyclerPlants.adapter as PlantAdapter).submitList(plants)
            }
        }

        // Observe Loading State (Show/Hide Spinner)
        lifecycleScope.launch {
            viewModel.isIdentifying.collect { isLoading ->
                loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
                fabAddPlant.isEnabled = !isLoading
            }
        }

        // Observe API Response (Log & Notify)
        lifecycleScope.launch {
            viewModel.identificationResult.collect { result ->
                result?.let {
                    Log.i("PLANT_API", "Successfully identified: ${it.masterData.speciesName}")
                    Log.i("PLANT_API", "OpenAI Fact: ${it.masterData.fact}")

                    Toast.makeText(this@MainActivity,
                        "Added ${it.masterData.speciesName}!", Toast.LENGTH_LONG).show()

                    // Crucial: reset result so it doesn't trigger again on config change
                    viewModel.resetIdentificationResult()
                }
            }
        }
    }
}