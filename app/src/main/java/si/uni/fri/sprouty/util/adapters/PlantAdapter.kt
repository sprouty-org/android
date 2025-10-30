package si.uni.fri.sprouty.util.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import si.uni.fri.sprouty.databinding.ItemPlantBinding
import si.uni.fri.sprouty.util.datatypes.Plant

class PlantAdapter(
    private val plants: List<Plant>,
    private val onClick: (Plant) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    inner class PlantViewHolder(val binding: ItemPlantBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val binding = ItemPlantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        holder.binding.textPlantName.text = plant.name
        holder.binding.imagePlant.setImageResource(plant.imageRes)
        holder.binding.root.setOnClickListener { onClick(plant) }
    }

    override fun getItemCount() = plants.size
}