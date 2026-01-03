package si.uni.fri.sprouty.util.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import si.uni.fri.sprouty.R
import si.uni.fri.sprouty.data.model.Plant

class PlantAdapter(
    private val onItemClick: (Plant) -> Unit,
    private val onConnectSensorClick: (Plant) -> Unit,
    private val onRenameClick: (Plant) -> Unit,
    private val onDeleteClick: (Plant) -> Unit,
    private val onDisconnectSensorClick: (Plant) -> Unit
) : ListAdapter<Plant, PlantAdapter.PlantViewHolder>(PlantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view, onItemClick, onConnectSensorClick, onRenameClick, onDeleteClick, onDisconnectSensorClick)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PlantViewHolder(
        itemView: View,
        val onClick: (Plant) -> Unit,
        val onConnectClick: (Plant) -> Unit,
        val onRename: (Plant) -> Unit,
        val onDelete: (Plant) -> Unit,
        val onDisconnect: (Plant) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val imgPlant: ImageView = itemView.findViewById(R.id.imgPlant)
        private val tvName: TextView = itemView.findViewById(R.id.tvPlantName)
        private val tvSpecies: TextView = itemView.findViewById(R.id.tvSpecies)
        private val btnOptions: ImageButton = itemView.findViewById(R.id.btnOptions) // New

        private val btnConnect: Button = itemView.findViewById(R.id.btnConnectSensor)
        private val layoutStats: LinearLayout = itemView.findViewById(R.id.layoutSensorStats)
        private val tvMoisture: TextView = itemView.findViewById(R.id.tvMoisture)
        private val tvTemp: TextView = itemView.findViewById(R.id.tvTemp)
        private val tvHumidity: TextView = itemView.findViewById(R.id.tvHumidity)

        fun bind(plant: Plant) {
            tvName.text = plant.customName ?: plant.speciesName
            tvSpecies.text = plant.speciesName

            imgPlant.load(plant.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_missing_image)
                error(R.drawable.ic_missing_image)
            }

            // --- Menu Logic ---
            btnOptions.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.inflate(R.menu.plant_item_menu)

                // Only show "Disconnect" if a sensor is actually there
                val hasSensor = !plant.connectedSensorId.isNullOrEmpty()
                popup.menu.findItem(R.id.action_disconnect_sensor).isVisible = hasSensor

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_rename -> { onRename(plant); true }
                        R.id.action_delete -> { onDelete(plant); true }
                        R.id.action_disconnect_sensor -> { onDisconnect(plant); true }
                        else -> false
                    }
                }
                popup.show()
            }

            // --- Sensor Display Logic ---
            val isSensorConnected = !plant.connectedSensorId.isNullOrEmpty()
            if (isSensorConnected) {
                btnConnect.isVisible = false
                layoutStats.isVisible = true
                tvMoisture.text = "💧 ${plant.currentHumiditySoil.toInt()}%"
                tvTemp.text = "🌡️ ${plant.currentTemperature ?: "--"}°C"
                tvHumidity.text = "☁️ ${plant.currentHumidityAir?.toInt()}%"
            } else {
                btnConnect.isVisible = true
                layoutStats.isVisible = false
                btnConnect.setOnClickListener { onConnectClick(plant) }
            }

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