package tech.rimuruchan.rewardchest.data

import org.bukkit.Location
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

@Suppress("UNCHECKED_CAST")
@SerializableAs("ChestData")
class ChestData(val locations: MutableList<Location>) : ConfigurationSerializable {
    var name: String = ""
    var items: MutableList<ItemStack> = ArrayList()
    var openedPlayer: Player? = null
    var refreshTime = 60
    var disappearTime = 40
    var hologramEntity: ArmorStand? = null

    override fun serialize(): MutableMap<String, Any> = HashMap<String, Any>().apply {
        this["name"] = name
        this["locations"] = locations
        this["items"] = items
        this["refreshTime"] = refreshTime
        this["disappearTime"] = disappearTime
    }

    companion object {

        @JvmStatic
        fun deserialize(args: MutableMap<String, Any>) =
            ChestData(args["locations"] as MutableList<Location>? ?: ArrayList()).apply {
                name = args["name"] as String
                items = args["items"] as MutableList<ItemStack>? ?: ArrayList()
                refreshTime = args["refreshTime"] as Int
                disappearTime = args["disappearTime"] as Int
            }
    }
}