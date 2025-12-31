package si.uni.fri.sprouty.util.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.data.model.Plant

// Add a second callback for the connect button
class PlantAdapter(
    private val onItemClick: (Plant) -> Unit,
    private val onConnectSensorClick: (Plant) -> Unit
) : ListAdapter<Plant, PlantAdapter.PlantViewHolder>(PlantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view, onItemClick, onConnectSensorClick)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PlantViewHolder(
        itemView: View,
        val onClick: (Plant) -> Unit,
        val onConnectClick: (Plant) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val imgPlant: ImageView = itemView.findViewById(R.id.imgPlant)
        private val tvName: TextView = itemView.findViewById(R.id.tvPlantName)
        private val tvSpecies: TextView = itemView.findViewById(R.id.tvSpecies)

        // New UI Elements
        private val btnConnect: Button = itemView.findViewById(R.id.btnConnectSensor)
        private val layoutStats: LinearLayout = itemView.findViewById(R.id.layoutSensorStats)
        private val tvMoisture: TextView = itemView.findViewById(R.id.tvMoisture)
        private val tvTemp: TextView = itemView.findViewById(R.id.tvTemp)
        private val tvHumidity: TextView = itemView.findViewById(R.id.tvHumidity)

        fun bind(plant: Plant) {
            tvName.text = plant.customName ?: plant.speciesName
            tvSpecies.text = plant.speciesName

            // Image Loading
            imgPlant.load(plant.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_missing_image)
                error(R.drawable.ic_missing_image)
            }

            // --- Logic for Sensor Connection ---
            val isSensorConnected = !plant.connectedSensorId.isNullOrEmpty()

            if (isSensorConnected) {
                // HIDE Button, SHOW Stats
                btnConnect.isVisible = false
                layoutStats.isVisible = true

                // Populate with data from Firestore (assuming these fields exist on your Plant model)
                // Note: Values might be 0.0 if no data has arrived yet
                tvMoisture.text = "💧 ${plant.currentHumiditySoil.toInt()}%"
                tvTemp.text = "🌡️ ${plant.currentTemperature ?: "--"}°C"
                // If you don't have air humidity on the Plant object yet, hide it or use placeholder
                tvHumidity.text = "☁️ Data OK"
            } else {
                // SHOW Button, HIDE Stats
                btnConnect.isVisible = true
                layoutStats.isVisible = false

                btnConnect.setOnClickListener {
                    onConnectClick(plant)
                }
            }

            // General item click
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