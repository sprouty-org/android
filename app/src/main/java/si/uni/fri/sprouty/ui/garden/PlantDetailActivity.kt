package si.uni.fri.sprouty.ui.garden

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.databinding.ActivityPlantDetailBinding

class PlantDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlantDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlantDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val imageUrl = intent.getStringExtra("PLANT_IMAGE_URL")

        // Use Coil to load the image
        binding.imagePlantDetail.load(imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_flower_rose) // Your placeholder
            error(R.drawable.ic_flower_rose)       // Fallback if load fails
        }

        // Extract Data
        val species = intent.getStringExtra("SPECIES_NAME") ?: "Unknown"
        val custom = intent.getStringExtra("CUSTOM_NAME")
        val fact = intent.getStringExtra("PLANT_FACT") ?: "No botanical details available."
        val tox = intent.getStringExtra("PLANT_TOX") ?: "Unknown"
        val growth = intent.getStringExtra("PLANT_GROWTH") ?: "Unknown"
        val soil = intent.getStringExtra("PLANT_SOIL") ?: "Standard Potting Mix"
        val type = intent.getStringExtra("PLANT_TYPE") ?: "Plant"

        // Bind to UI
        binding.tvDetailTitle.text = custom ?: species
        binding.tvDetailFact.text = fact
        binding.chipWater.text = "Every ${intent.getIntExtra("WATER_INTERVAL", 0)} days"
        binding.chipLight.text = intent.getStringExtra("LIGHT_LEVEL") ?: "Medium Light"

        binding.tvDetailAttributes.text = """
            • Type: $type
            • Species: $species
            • Toxicity: $tox
            • Growth Habit: $growth
            • Preferred Soil: $soil
        """.trimIndent()
    }
}