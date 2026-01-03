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
import si.uni.fri.sprouty.util.limiters.ActionRateLimiter // Import the limiter

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

        // 2. Extract Data from Intent
        val plantId = intent.getStringExtra("FIREBASE_ID") ?: ""
        val imageUrl = intent.getStringExtra("PLANT_IMAGE_URL")
        val species = intent.getStringExtra("SPECIES_NAME") ?: "Unknown Species"
        val custom = intent.getStringExtra("CUSTOM_NAME")
        val fact = intent.getStringExtra("PLANT_FACT") ?: "No botanical details available."

        val type = intent.getStringExtra("PLANT_TYPE") ?: "Unknown"
        val life = intent.getStringExtra("PLANT_LIFE") ?: "Unknown"
        val growth = intent.getStringExtra("PLANT_GROWTH") ?: "Moderate"
        val maxHeight = intent.getIntExtra("PLANT_HEIGHT", 0)
        val difficulty = intent.getStringExtra("CARE_DIFFICULTY") ?: "Moderate"

        val minT = intent.getIntExtra("MIN_TEMP", 0)
        val maxT = intent.getIntExtra("MAX_TEMP", 0)
        val minAirH = intent.getIntExtra("MIN_AIR_HUMIDITY", 0)
        val maxAirH = intent.getIntExtra("MAX_AIR_HUMIDITY", 0)
        val minSoilH = intent.getIntExtra("MIN_SOIL_HUMIDITY", 0)
        val maxSoilH = intent.getIntExtra("MAX_SOIL_HUMIDITY", 0)

        val light = intent.getStringExtra("LIGHT_LEVEL") ?: "No data"
        val soil = intent.getStringExtra("PLANT_SOIL") ?: "Standard Mix"
        val tox = intent.getStringExtra("PLANT_TOX") ?: "Unknown"
        val fruit = intent.getStringExtra("PLANT_FRUIT") ?: "None"

        // Use double newline for uses list
        val usesList = intent.getStringArrayListExtra("PLANT_USES")
        val usesFormatted = usesList?.joinToString("\n\n") ?: "No uses available."

        val notifEnabled = intent.getBooleanExtra("NOTIF_ENABLED", true)

        // 3. Bind Header & Image
        binding.imagePlantDetail.load(imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_missing_image)
            error(R.drawable.ic_missing_image)
        }
        binding.tvDetailTitle.text = if (!custom.isNullOrBlank()) custom else species
        binding.tvDetailSpecies.text = species

        binding.switchNotifications.setOnCheckedChangeListener { v, checked ->
            handleNotifChange(v, checked, plantId)
        }

        // 5. Water Plant Logic with Rate Limiter
        binding.btnWaterPlant.apply {
            // Fix icon color (remove white tint)
            iconTint = null
            setOnClickListener {
                if (ActionRateLimiter.canPerformAction("water_$plantId", 5000)) {
                    waterPlant(plantId)
                } else {
                    Toast.makeText(this@PlantDetailActivity, "Wait a few seconds...", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 6. Fun Fact
        binding.tvDetailFact.text = fact

        // 7. Bind Cards
        binding.cardDifficulty.apply {
            cardTitle.text = "Care Difficulty"
            cardIcon.setImageResource(R.drawable.ic_difficulty)
            cardData.text = difficulty
        }

        binding.cardBotanical.apply {
            cardTitle.text = "Botanical Overview"
            cardIcon.setImageResource(R.drawable.ic_info)
            cardData.text = "Type: $type\nLifespan: $life\nGrowth: $growth\nMax Height: ${maxHeight}cm"
        }

        binding.cardTemp.apply {
            cardTitle.text = "Temperature"
            cardIcon.setImageResource(R.drawable.ic_temperature)
            cardData.text = "$minT°C - $maxT°C"
        }

        binding.cardHumidity.apply {
            cardTitle.text = "Humidity Range"
            cardIcon.setImageResource(R.drawable.ic_humidity)
            cardData.text = "Air: $minAirH% - $maxAirH%\nSoil: $minSoilH% - $maxSoilH%"
        }

        binding.cardSunlight.apply {
            cardTitle.text = "Sunlight"
            cardIcon.setImageResource(R.drawable.ic_sunlight)
            cardData.text = light
        }

        binding.cardSoil.apply {
            cardTitle.text = "Recommended Soil"
            cardIcon.setImageResource(R.drawable.ic_soil)
            cardData.text = soil
        }

        binding.cardToxicity.apply {
            cardTitle.text = "Toxicity"
            cardIcon.setImageResource(R.drawable.ic_toxicity)
            cardData.text = tox
        }

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
                    Toast.makeText(this@PlantDetailActivity, "Notifications updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@PlantDetailActivity, "Failed to update", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Detail", "Error: ${e.message}")
            }
        }
    }

    private fun handleNotifChange(buttonView: android.widget.CompoundButton, isChecked: Boolean, plantId: String) {
        if (ActionRateLimiter.canPerformAction("notif_$plantId", 3000)) {
            toggleNotifications(plantId, isChecked)
        } else {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show()

            buttonView.setOnCheckedChangeListener(null) // Unbind
            buttonView.isChecked = !isChecked          // Revert
            buttonView.setOnCheckedChangeListener { v, checked ->
                handleNotifChange(v, checked, plantId) // Rebind
            }
        }
    }

    private fun waterPlant(id: String) {
        lifecycleScope.launch {
            try {
                val response = plantApiService.waterPlant(id)
                if (response.isSuccessful) {
                    Toast.makeText(this@PlantDetailActivity, "Watering timer reset!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@PlantDetailActivity, "Error resetting timer", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Detail", "Error: ${e.message}")
            }
        }
    }
}