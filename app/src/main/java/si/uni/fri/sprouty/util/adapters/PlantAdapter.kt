package si.uni.fri.sprouty.util.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.data.model.Plant

class PlantAdapter(private val onItemClick: (Plant) -> Unit) :
    ListAdapter<Plant, PlantAdapter.PlantViewHolder>(PlantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PlantViewHolder(itemView: View, val onClick: (Plant) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val imgPlant: ImageView = itemView.findViewById(R.id.imgPlant)
        private val tvName: TextView = itemView.findViewById(R.id.tvPlantName)
        private val tvSpecies: TextView = itemView.findViewById(R.id.tvSpecies)
        private val tvWater: TextView = itemView.findViewById(R.id.tvWateringInfo)

        fun bind(plant: Plant) {
            tvName.text = plant.customName ?: plant.speciesName
            tvSpecies.text = plant.speciesName
            tvWater.text = "Water every ${plant.targetWateringInterval} days"

            // Load image (using placeholder for better UX)
            imgPlant.load(plant.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_flower_rose)
                error(R.drawable.ic_flower_rose)
            }

            // Move this here so it works immediately
            itemView.setOnClickListener { onClick(plant) }
        }
    }

    class PlantDiffCallback : DiffUtil.ItemCallback<Plant>() {
        override fun areItemsTheSame(oldItem: Plant, newItem: Plant): Boolean =
            oldItem.firebaseId == newItem.firebaseId

        override fun areContentsTheSame(oldItem: Plant, newItem: Plant): Boolean =
            oldItem == newItem
    }
}