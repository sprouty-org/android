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
import si.uni.fri.sprouty.util.limiters.ActionRateLimiter

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


        binding.btnBack.setOnClickListener { finish() }


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
        val wateringInterval = intent.getIntExtra("WATERING_INTERVAL", 7)


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


        val usesList = intent.getStringArrayListExtra("PLANT_USES")
        val mergedUses = mutableListOf<String>()

        usesList?.forEach { use ->
            val trimmedUse = use.trim()
            if (trimmedUse.isNotEmpty()) {
                if (mergedUses.isNotEmpty() && trimmedUse[0].isLowerCase()) {
                    val lastIndex = mergedUses.size - 1
                    mergedUses[lastIndex] = "${mergedUses[lastIndex]}, $trimmedUse"
                } else {
                    mergedUses.add(trimmedUse)
                }
            }
        }
        mergedUses.removeAll { it.isBlank() }

        val usesFormatted = if (mergedUses.isNotEmpty()) {
            mergedUses.joinToString("\n\n")
        } else {
            "No common uses"
        }

        val notifEnabled = intent.getBooleanExtra("NOTIF_ENABLED", true)
        binding.switchNotifications.isChecked = notifEnabled


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

        binding.plantWateringIntervalText.text = "This plant needs to be watered every $wateringInterval days unless the smart sensor says otherwise.\n\n" +
                "Please click the button below when you water the plant so we can send you a notification when it's time to water again."
        binding.btnWaterPlant.apply {
            setOnClickListener {
                if (ActionRateLimiter.canPerformAction("water_$plantId", 5000)) {
                    waterPlant(plantId)
                } else {
                    Toast.makeText(this@PlantDetailActivity, "Wait a few seconds...", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.tvDetailFact.text = fact


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

            buttonView.setOnCheckedChangeListener(null)
            buttonView.isChecked = !isChecked
            buttonView.setOnCheckedChangeListener { v, checked ->
                handleNotifChange(v, checked, plantId)
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