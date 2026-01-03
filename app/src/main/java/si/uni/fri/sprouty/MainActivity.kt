package si.uni.fri.sprouty

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import si.uni.fri.sprouty.data.network.NotificationHelper

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

        checkNotificationPermission()

        viewModel.refreshData()
        //NotificationHelper.triggerLocalNotification(this, "Local Test", "This didn't use the backend!")
    }

    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) {

            // Request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }
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

        // Initialize Adapter with all 5 callbacks
        plantAdapter = PlantAdapter(
            onItemClick = { plant -> navigateToDetail(plant) },
            onConnectSensorClick = { plant -> showConnectSensorDialog(plant) },
            onRenameClick = { plant -> showRenameDialog(plant) },
            onDeleteClick = { plant -> showDeleteConfirmation(plant) },
            onDisconnectSensorClick = { plant -> showDisconnectConfirmation(plant) }
        )

        recyclerPlants.adapter = plantAdapter

        fabAddPlant.setOnClickListener { showImageSourceDialog() }

        findViewById<ImageView>(R.id.icon_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    // --- NEW DIALOG FUNCTIONALITIES ---

    private fun showRenameDialog(plant: Plant) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Rename Plant")

        val input = EditText(this)
        input.setText(plant.customName ?: plant.speciesName)
        input.setSelectAllOnFocus(true)

        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(60, 20, 60, 20)
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton("Save") { _, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                viewModel.renamePlant(plant.firebaseId, newName)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showDisconnectConfirmation(plant: Plant) {
        AlertDialog.Builder(this)
            .setTitle("Disconnect Sensor")
            .setMessage("Are you sure you want to remove the sensor from ${plant.customName ?: plant.speciesName}?")
            .setPositiveButton("Disconnect") { _, _ ->
                viewModel.disconnectSensorFromPlant(plant.firebaseId)
                Toast.makeText(this, "Sensor disconnected", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(plant: Plant) {
        AlertDialog.Builder(this)
            .setTitle("Remove Plant")
            .setMessage("Are you sure you want to delete ${plant.customName ?: plant.speciesName} from your garden?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePlant(plant.firebaseId)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_menu_delete)
            .show()
    }

    private fun showConnectSensorDialog(plant: Plant) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Connect Sensor")
        builder.setMessage("Enter the 12-character Sensor ID")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.filters = arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(12))
        input.hint = "AABBCCDDEEFF"

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
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    // --- NAVIGATION AND VIEWMODEL OBSERVATION ---

    private fun navigateToDetail(plant: Plant) {
        val intent = Intent(this, PlantDetailActivity::class.java).apply {
            // Basic Info
            putExtra("FIREBASE_ID", plant.firebaseId)
            putExtra("PLANT_IMAGE_URL", plant.imageUrl)
            putExtra("SPECIES_NAME", plant.speciesName)
            putExtra("CUSTOM_NAME", plant.customName)
            putExtra("PLANT_FACT", plant.botanicalFact)

            // Botanical Info
            putExtra("PLANT_TYPE", plant.botanicalType)
            putExtra("PLANT_LIFE", plant.lifecycle)
            putExtra("PLANT_GROWTH", plant.growthHabit)
            putExtra("PLANT_HEIGHT", plant.maxHeight)
            putExtra("CARE_DIFFICULTY", plant.careDifficulty)

            // Environment & Care
            putExtra("MIN_TEMP", plant.minTemp)
            putExtra("MAX_TEMP", plant.maxTemp)
            putExtra("MIN_AIR_HUMIDITY", plant.minAirHumidity)
            putExtra("MAX_AIR_HUMIDITY", plant.maxAirHumidity)
            putExtra("MIN_SOIL_HUMIDITY", plant.minSoilHumidity)
            putExtra("MAX_SOIL_HUMIDITY", plant.maxSoilHumidity)
            putExtra("LIGHT_LEVEL", plant.requiredLightLevel)
            putExtra("PLANT_SOIL", plant.soilType)

            // Extras
            putExtra("PLANT_TOX", plant.toxicity)
            putExtra("PLANT_FRUIT", plant.fruitInfo)
            putExtra("PLANT_USES", ArrayList(plant.uses))
            putExtra("NOTIF_ENABLED", plant.notificationsEnabled)
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