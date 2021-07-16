package tech.rimuruchan.rewardchest.gui

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.entity.minecart.StorageMinecart
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import tech.rimuruchan.rewardchest.config

class DeathChestGui(player: Player, val  entity: StorageMinecart) : InventoryHolder {

    private val inventory = Bukkit.createInventory(this, 54, "${player.displayName} 的死亡箱子").apply {
        val list = player.inventory.contents.toMutableList().apply { removeIf { it == null } }
        val dropTimes = config.getInt("deathChest.dropPercent").toDouble() / 100 * list.size
        for (i in 1..dropTimes.toInt()){
            list.shuffle()
            list.removeAt(0)
        }
        addItem(*list.toTypedArray())
    }

    override fun getInventory(): Inventory {
        return inventory
    }
}