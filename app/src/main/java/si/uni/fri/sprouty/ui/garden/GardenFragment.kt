package si.uni.fri.sprouty.ui.garden

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.adapters.PlantAdapter
import si.uni.fri.sprouty.databinding.FragmentGardenBinding
import si.uni.fri.sprouty.datatypes.Plant

class GardenFragment : Fragment(R.layout.fragment_garden) {

    private var _binding: FragmentGardenBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlantAdapter
    private var dX = 0f
    private var dY = 0f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGardenBinding.bind(view)

        setupRecyclerView()
        setupFab()
    }

    /** Setup 2-column plant list */
    private fun setupRecyclerView() {
        val dummyPlants = listOf(
            Plant("Rose", R.drawable.ic_flower_rose)
        )

        adapter = PlantAdapter(dummyPlants) { plant ->
            // On plant click → open details
            val intent = Intent(requireContext(), PlantDetailActivity::class.java)
            intent.putExtra("plant_name", plant.name)
            intent.putExtra("plant_image", plant.imageRes)
            startActivity(intent)
        }

        binding.recyclerPlants.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerPlants.adapter = adapter
    }

    /** Setup movable green + button */
    private fun setupFab() {
        val fab = binding.fabAddPlant
        fab.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    v.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                    true
                }
                else -> false
            }
        }

        fab.setOnClickListener {
            // Example: open a screen to add a new plant
            // You can replace this with your own AddPlantFragment or dialog
            // Toast.makeText(requireContext(), "Add new plant clicked", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}