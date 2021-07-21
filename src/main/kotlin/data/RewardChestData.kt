package tech.rimuruchan.rewardchest.data

import org.bukkit.Location
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs

@Suppress("UNCHECKED_CAST")
@SerializableAs("RewardChestData")
class RewardChestData : ConfigurationSerializable {
    //data
    var name: String = ""
    var chests: MutableMap<String, Int> = hashMapOf()
    var locations: MutableList<Location> = arrayListOf()
    var refreshTime = 60
    var minGenAmount = 1
    var maxGenAmount = 1

    override fun serialize(): MutableMap<String, Any> = HashMap<String, Any>().apply {
        this["name"] = name
        this["chests"] = chests
        this["locations"] = locations
        this["refreshTime"] = refreshTime
        this["minGenAmount"] = minGenAmount
        this["maxGenAmount"] = maxGenAmount
    }

    companion object {

        @Suppress("unused")
        @JvmStatic
        fun deserialize(map: MutableMap<String, Any>) =
            RewardChestData().apply {
                name = map["name"] as String
                chests = map["chests"] as MutableMap<String, Int>? ?: hashMapOf()
                locations = map["locations"] as MutableList<Location>? ?: arrayListOf()
                refreshTime = map["refreshTime"] as Int
                minGenAmount = map["minGenAmount"] as Int
                maxGenAmount = map["maxGenAmount"] as Int
            }
    }
}