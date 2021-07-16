package tech.rimuruchan.rewardchest.data

import org.bukkit.Location
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player

@Suppress("UNCHECKED_CAST")
@SerializableAs("SChestData")
class SChestData(val location: Location) : ConfigurationSerializable {
    var name: String = ""
    var chests: MutableMap<String, Int> = hashMapOf()
    var openedPlayer: Player? = null
    var refreshTime = 60
    var disappearTime = 40
    var hologramEntity: ArmorStand? = null

    override fun serialize(): MutableMap<String, Any> = HashMap<String, Any>().apply {
        this["name"] = name
        this["location"] = location
        this["chests"] = chests
        this["refreshTime"] = refreshTime
        this["disappearTime"] = disappearTime
    }

    companion object {

        @JvmStatic
        fun deserialize(args: MutableMap<String, Any>) =
            SChestData(args["location"] as Location).apply {
                name = args["name"] as String
                chests = args["chests"] as MutableMap<String, Int>? ?: hashMapOf()
                refreshTime = args["refreshTime"] as Int
                disappearTime = args["disappearTime"] as Int
            }
    }


}