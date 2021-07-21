package tech.rimuruchan.rewardchest.gui

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import tech.rimuruchan.rewardchest.colored
import tech.rimuruchan.rewardchest.config

class DeathChestGui(player: Player, val entity: ArmorStand) : InventoryHolder {

    private val inventory = Bukkit.createInventory(this, 54, "&c${player.displayName} 的死亡箱子".colored()).apply {
        val playerInventory = player.inventory
        val items = (0..40).filter { playerInventory.getItem(it) != null }.toMutableList()
        val availableSlots = (0..53).toMutableList()

        val dropTimes = config.getInt("deathChest.dropPercent").toDouble() / 100 * items.size

        for (i in 1..dropTimes.toInt()) {
            items.shuffle()
            availableSlots.shuffle()
            val drop = items.removeAt(0)
            setItem(availableSlots.removeAt(0), playerInventory.getItem(drop))
            playerInventory.setItem(drop, ItemStack(Material.AIR))
        }

    }

    override fun getInventory(): Inventory {
        return inventory
    }
}