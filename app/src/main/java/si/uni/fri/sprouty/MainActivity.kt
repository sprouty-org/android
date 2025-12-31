package si.uni.fri.sprouty

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
import si.uni.fri.sprouty.data.model.Plant

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: PlantViewModel
    private lateinit var recyclerPlants: RecyclerView
    private lateinit var fabAddPlant: FloatingActionButton
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var plantAdapter: PlantAdapter

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageUri(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let { viewModel.identifyAndAddPlant(it, applicationContext) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupDependencies()
        setupUI()
        observeViewModel()

        viewModel.refreshData()
    }

    private fun setupDependencies() {
        val plantDao = AppDatabase.getDatabase(applicationContext).plantDao()
        val apiService = NetworkModule.provideRetrofit(applicationContext).create(PlantApiService::class.java)
        val repository = PlantRepository(plantDao, apiService)

        viewModel = ViewModelProvider(this, PlantViewModelFactory(repository))[PlantViewModel::class.java]
    }

    private fun setupUI() {
        recyclerPlants = findViewById(R.id.recyclerPlants)
        fabAddPlant = findViewById(R.id.fabAddPlant)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        recyclerPlants.layoutManager = LinearLayoutManager(this)

        // Initialize Adapter with two callbacks
        plantAdapter = PlantAdapter(
            onItemClick = { plant -> navigateToDetail(plant) },
            onConnectSensorClick = { plant -> showConnectSensorDialog(plant) }
        )

        recyclerPlants.adapter = plantAdapter

        fabAddPlant.setOnClickListener { showImageSourceDialog() }

        findViewById<ImageView>(R.id.icon_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun showConnectSensorDialog(plant: Plant) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Connect Sensor")
        builder.setMessage("Enter the MAC Address of your sensor (e.g., AABBCCDDEEFF)")

        // Programmatically create EditText with margins
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.filters = arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(12))
        input.hint = "Sensor ID"

        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(60, 20, 60, 20)
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton("Connect") { _, _ ->
            val sensorId = input.text.toString().trim()
            if (sensorId.isNotEmpty()) {
                viewModel.connectSensorToPlant(plant.firebaseId, sensorId)
                Toast.makeText(this, "Connecting to $sensorId...", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun navigateToDetail(plant: Plant) {
        val intent = Intent(this, PlantDetailActivity::class.java).apply {
            putExtra("FIREBASE_ID", plant.firebaseId)
            putExtra("SPECIES_NAME", plant.speciesName)
            putExtra("CUSTOM_NAME", plant.customName)
            putExtra("PLANT_IMAGE_URL", plant.imageUrl)
            putExtra("WATER_INTERVAL", plant.targetWateringInterval)
            putExtra("LIGHT_LEVEL", plant.requiredLightLevel)
            putExtra("PLANT_HEIGHT", plant.maxHeight)
            putExtra("PLANT_FACT", plant.botanicalFact)
            putExtra("PLANT_TOX", plant.toxicity)
            putExtra("PLANT_GROWTH", plant.growthHabit)
            putExtra("PLANT_SOIL", plant.soilType)
            putExtra("PLANT_TYPE", plant.botanicalType)
            putExtra("PLANT_FRUIT", plant.fruitInfo)
            putExtra("NOTIF_ENABLED", plant.notificationsEnabled)
            putExtra("MIN_TEMP", plant.minTemp)
            putExtra("MAX_TEMP", plant.maxTemp)
            putExtra("AIR_HUMIDITY", "${plant.minAirHumidity} - ${plant.maxAirHumidity}%")
            putExtra("SOIL_HUMIDITY", "${plant.minSoilHumidity} - ${plant.maxSoilHumidity}%")
            putExtra("PLANT_LIFE", plant.lifecycle)
        }
        startActivity(intent)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.plantList.collect { plants ->
                plantAdapter.submitList(plants)
            }
        }

        lifecycleScope.launch {
            viewModel.isIdentifying.collect { isLoading ->
                loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
                fabAddPlant.isEnabled = !isLoading
            }
        }

        lifecycleScope.launch {
            viewModel.identificationResult.collect { result ->
                result?.let {
                    Toast.makeText(this@MainActivity, "Added ${it.masterPlant.speciesName}!", Toast.LENGTH_SHORT).show()
                    viewModel.resetIdentificationResult()
                }
            }
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
            }.show()
    }

    private fun handleImageUri(uri: Uri) {
        try {
            val source = ImageDecoder.createSource(contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source)
            viewModel.identifyAndAddPlant(bitmap, applicationContext)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error decoding image", e)
        }
    }
}