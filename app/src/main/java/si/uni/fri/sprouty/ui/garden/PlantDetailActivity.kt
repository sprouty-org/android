package si.uni.fri.sprouty.ui.garden

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import kotlinx.coroutines.launch
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.databinding.ActivityPlantDetailBinding
import si.uni.fri.sprouty.data.network.PlantApiService
import si.uni.fri.sprouty.util.network.NetworkModule


class PlantDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlantDetailBinding
    private lateinit var plantApiService: PlantApiService


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlantDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        plantApiService = NetworkModule.provideRetrofit(applicationContext)
            .create(PlantApiService::class.java)

        // 1. Setup Navigation
        binding.btnBack.setOnClickListener { finish() }

        // 2. Extract Data from Intent (Matching your updated navigateToDetail keys)
        val plantId = intent.getStringExtra("FIREBASE_ID") ?: ""
        val imageUrl = intent.getStringExtra("PLANT_IMAGE_URL")
        val species = intent.getStringExtra("SPECIES_NAME") ?: "Unknown Species"
        val custom = intent.getStringExtra("CUSTOM_NAME")
        val fact = intent.getStringExtra("PLANT_FACT") ?: "No botanical details available."

        // Botanical Info
        val type = intent.getStringExtra("PLANT_TYPE") ?: "Unknown"
        val life = intent.getStringExtra("PLANT_LIFE") ?: "Unknown"
        val growth = intent.getStringExtra("PLANT_GROWTH") ?: "Moderate"
        val maxHeight = intent.getIntExtra("PLANT_HEIGHT", 0)
        val difficulty = intent.getStringExtra("CARE_DIFFICULTY") ?: "Moderate"

        // Environment & Care
        val minT = intent.getIntExtra("MIN_TEMP", 0)
        val maxT = intent.getIntExtra("MAX_TEMP", 0)

        // Handling split humidity keys from navigateToDetail
        val minAirH = intent.getIntExtra("MIN_AIR_HUMIDITY", 0)
        val maxAirH = intent.getIntExtra("MAX_AIR_HUMIDITY", 0)
        val minSoilH = intent.getIntExtra("MIN_SOIL_HUMIDITY", 0)
        val maxSoilH = intent.getIntExtra("MAX_SOIL_HUMIDITY", 0)

        val light = intent.getStringExtra("LIGHT_LEVEL") ?: "No data"
        val soil = intent.getStringExtra("PLANT_SOIL") ?: "Standard Mix"

        // Extras
        val tox = intent.getStringExtra("PLANT_TOX") ?: "Unknown"
        val fruit = intent.getStringExtra("PLANT_FRUIT") ?: "None"
        val usesList = intent.getStringArrayListExtra("PLANT_USES")
        val usesFormatted = usesList?.joinToString("\n") ?: "No uses at all..."
        val notifEnabled = intent.getBooleanExtra("NOTIF_ENABLED", true)

        // 3. Bind Header & Image
        binding.imagePlantDetail.load(imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_missing_image)
            error(R.drawable.ic_missing_image)
        }
        binding.tvDetailTitle.text = if (!custom.isNullOrBlank()) custom else species
        binding.tvDetailSpecies.text = species

        // 4. Notification Toggle
        binding.switchNotifications.isChecked = notifEnabled
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            toggleNotifications(plantId, isChecked)
        }

        // 3. Water Plant Logic
        binding.btnWaterPlant.setOnClickListener {
            waterPlant(plantId)
        }

        // 5. Fun Fact
        binding.tvDetailFact.text = fact

        // 6. Bind Cards

        binding.cardDifficulty.apply {
            cardTitle.text = "Care Difficulty"
            cardIcon.setImageResource(R.drawable.ic_difficulty)
            cardData.text = difficulty
        }

        // Botanical Card
        binding.cardBotanical.apply {
            cardTitle.text = "Botanical Overview"
            cardIcon.setImageResource(R.drawable.ic_info)
            cardData.text = "Type: $type\nLifespan: $life\nGrowth: $growth\nMax Height: ${maxHeight}cm"
        }

        // Temperature Card
        binding.cardTemp.apply {
            cardTitle.text = "Temperature"
            cardIcon.setImageResource(R.drawable.ic_temperature)
            cardData.text = "$minT°C - $maxT°C"
        }

        // Humidity Card - Correctly combining the min/max values
        binding.cardHumidity.apply {
            cardTitle.text = "Humidity Range"
            cardIcon.setImageResource(R.drawable.ic_humidity)
            cardData.text = "Air: $minAirH% - $maxAirH%\nSoil: $minSoilH% - $maxSoilH%"
        }

        // Sunlight Card
        binding.cardSunlight.apply {
            cardTitle.text = "Sunlight"
            cardIcon.setImageResource(R.drawable.ic_sunlight)
            cardData.text = light
        }

        // Soil Card
        binding.cardSoil.apply {
            cardTitle.text = "Recommended Soil"
            cardIcon.setImageResource(R.drawable.ic_soil)
            cardData.text = soil
        }

        // Toxicity Card
        binding.cardToxicity.apply {
            cardTitle.text = "Toxicity"
            cardIcon.setImageResource(R.drawable.ic_toxicity)
            cardData.text = tox
        }

        // Fruit Card
        binding.cardFruit.apply {
            cardTitle.text = "Fruits"
            cardIcon.setImageResource(R.drawable.ic_fruit)
            cardData.text = fruit
        }

        binding.cardUses.apply {
            cardTitle.text = "Common Uses"
            cardIcon.setImageResource(R.drawable.ic_usage)
            cardData.text = usesFormatted
        }
    }

    private fun toggleNotifications(id: String, enabled: Boolean) {
        lifecycleScope.launch {
            try {
                val response = plantApiService.toggleNotifications(id, enabled)
                if (response.isSuccessful) {
                    Toast.makeText(this@PlantDetailActivity, "Notifications ${if (enabled) "enabled" else "muted"}", Toast.LENGTH_SHORT).show()
                } else {
                    binding.switchNotifications.isChecked = !enabled // Revert on failure
                    Toast.makeText(this@PlantDetailActivity, "Failed to update settings", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.switchNotifications.isChecked = !enabled
                Log.e("Detail", "Error: ${e.message}")
            }
        }
    }

    private fun waterPlant(id: String) {
        lifecycleScope.launch {
            try {
                val response = plantApiService.waterPlant(id)
                if (response.isSuccessful) {
                    Toast.makeText(this@PlantDetailActivity, "Watering timer reset!", Toast.LENGTH_LONG).show()
                    // Optional: Update a "Last Watered" text field locally if you have one
                } else {
                    Toast.makeText(this@PlantDetailActivity, "Error resetting timer", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Detail", "Error: ${e.message}")
            }
        }
    }
}