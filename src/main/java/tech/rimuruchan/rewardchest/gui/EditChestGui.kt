package tech.rimuruchan.rewardchest.gui

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class EditChestGui(val chest: String, val locations: MutableList<Location>) : InventoryHolder {

    private val inventory = Bukkit.createInventory(this, 54, "请放入奖励物品 $chest")

    override fun getInventory(): Inventory {
        return inventory
    }
}