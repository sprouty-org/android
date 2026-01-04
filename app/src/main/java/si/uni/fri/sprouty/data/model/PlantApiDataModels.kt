package si.uni.fri.sprouty.data.model

import com.google.gson.annotations.SerializedName

data class GardenProfileResponse(
    val userPlants: List<UserPlant>,
    val masterPlants: List<MasterPlant>
)

data class PlantIdentificationResponse(
    @SerializedName("userPlant")
    val userPlant: UserPlant,

    @SerializedName("masterPlant")
    val masterPlant: MasterPlant
)