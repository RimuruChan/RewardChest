package tech.rimuruchan.rewardchest.data

import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs
import org.bukkit.inventory.ItemStack

@Suppress("UNCHECKED_CAST")
@SerializableAs("ChestData")
class ChestData : ConfigurationSerializable {
    //data
    var name: String = ""
    var displayName: String = ""
    var items: MutableList<ItemStack> = arrayListOf()
    var disappearTime = 40

    override fun serialize(): MutableMap<String, Any> = HashMap<String, Any>().apply {
        this["name"] = name
        this["displayName"] = displayName
        this["items"] = items
        this["disappearTime"] = disappearTime
    }

    companion object {

        @Suppress("unused")
        @JvmStatic
        fun deserialize(map: MutableMap<String, Any>) =
            ChestData().apply {
                name = map["name"] as String
                displayName = map["displayName"] as String
                items = map["items"] as MutableList<ItemStack>? ?: arrayListOf()
                disappearTime = map["disappearTime"] as Int
            }
    }
}