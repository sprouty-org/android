package si.uni.fri.sprouty.ui.garden

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.databinding.ActivityPlantDetailBinding

class PlantDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlantDetailBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlantDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Setup Navigation
        binding.btnBack.setOnClickListener { finish() }

        // 2. Extract Data from Intent
        val imageUrl = intent.getStringExtra("PLANT_IMAGE_URL")
        val species = intent.getStringExtra("SPECIES_NAME") ?: "Unknown Species"
        val custom = intent.getStringExtra("CUSTOM_NAME")
        val fact = intent.getStringExtra("PLANT_FACT") ?: "No botanical details available."

        // Botanical Info
        val type = intent.getStringExtra("PLANT_TYPE") ?: "Unknown"
        val life = intent.getStringExtra("PLANT_LIFE") ?: "Unknown"
        val growth = intent.getStringExtra("PLANT_GROWTH") ?: "Moderate"
        val maxHeight = intent.getIntExtra("PLANT_HEIGHT", 0)

        // Environment & Care
        val minT = intent.getIntExtra("MIN_TEMP", 0)
        val maxT = intent.getIntExtra("MAX_TEMP", 0)
        val airH = intent.getStringExtra("AIR_HUMIDITY") ?: "N/A"
        val soilH = intent.getStringExtra("SOIL_HUMIDITY") ?: "N/A"
        val light = intent.getStringExtra("LIGHT_LEVEL") ?: "No data"
        val soil = intent.getStringExtra("PLANT_SOIL") ?: "Standard Mix"

        // Extras
        val tox = intent.getStringExtra("PLANT_TOX") ?: "Unknown"
        val fruit = intent.getStringExtra("PLANT_FRUIT") ?: "None"
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
            // Handle notification toggle logic here or save to Database/Firebase
        }

        // 5. Fun Fact
        binding.tvDetailFact.text = fact

        // 6. Bind Cards (Accessing includes via binding)

        // Botanical Card
        binding.cardBotanical.apply {
            cardTitle.text = "Botanical Overview"
            // Ensure these icons exist in your drawable folder
            cardIcon.setImageResource(R.drawable.ic_info)
            cardData.text = "Type: $type\nLifespan: $life\nGrowth: $growth\nMax Height: ${maxHeight}cm"
        }

        // Temperature Card
        binding.cardTemp.apply {
            cardTitle.text = "Temperature"
            //set image to res/drawable/ic_temperature
            cardIcon.setImageResource(R.drawable.ic_temperature)
            cardData.text = "$minT°C - $maxT°C"
        }

        // Humidity Card
        binding.cardHumidity.apply {
            cardTitle.text = "Humidity"
            cardIcon.setImageResource(R.drawable.ic_humidity)
            cardData.text = "Air Humidity: $airH\nSoil Humidity: $soilH"
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

        // Toxicity & Fruits Card
        binding.cardToxicity.apply {
            cardTitle.text = "Toxicity"
            cardIcon.setImageResource(R.drawable.ic_toxicity)
            cardData.text = tox
        }

        // Toxicity & Fruits Card
        binding.cardFruit.apply {
            cardTitle.text = "Fruits"
            cardIcon.setImageResource(R.drawable.ic_fruit)
            cardData.text = fruit
        }
    }
}