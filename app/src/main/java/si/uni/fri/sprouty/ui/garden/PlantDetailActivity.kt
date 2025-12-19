package si.uni.fri.sprouty.ui.garden

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import si.uni.fri.sprouty.databinding.ActivityPlantDetailBinding

class PlantDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlantDetailBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlantDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Back Button
        binding.btnBack.setOnClickListener { finish() }

        // 2. Extract Data from Intent
        val speciesName = intent.getStringExtra("SPECIES_NAME") ?: "Unknown"
        val customName = intent.getStringExtra("CUSTOM_NAME")
        val fact = intent.getStringExtra("PLANT_FACT") ?: "Botanical data loading..."
        val water = intent.getIntExtra("WATER_INTERVAL", 0)
        val light = intent.getStringExtra("LIGHT_LEVEL") ?: "Medium Light"

        // 3. Bind to UI
        binding.tvDetailTitle.text = customName ?: speciesName
        binding.tvDetailFact.text = fact
        binding.chipWater.text = "Every $water days"
        binding.chipLight.text = light

        binding.tvDetailAttributes.text = """
            Species: $speciesName
            Toxicity: ${intent.getStringExtra("PLANT_TOX") ?: "N/A"}
            Growth: ${intent.getStringExtra("PLANT_GROWTH") ?: "N/A"}
            Soil: ${intent.getStringExtra("PLANT_SOIL") ?: "N/A"}
        """.trimIndent()
    }
}