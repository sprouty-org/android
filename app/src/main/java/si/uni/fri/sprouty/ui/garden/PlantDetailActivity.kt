package si.uni.fri.sprouty.ui.garden

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import si.uni.fri.sprouty.databinding.ActivityPlantDetailBinding

class PlantDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlantDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlantDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name = intent.getStringExtra("plant_name")
        val imageRes = intent.getIntExtra("plant_image", 0)

        binding.textPlantName.text = name
        binding.imagePlant.setImageResource(imageRes)

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> { /* show picture section */ }
                    1 -> { /* show lifecycle section */ }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
}