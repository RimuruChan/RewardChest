package tech.rimuruchan.rewardchest.gui

import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import tech.rimuruchan.rewardchest.colored

class EditChestGui(val chest: String) : InventoryHolder {

    private val inventory = Bukkit.createInventory(this, 54, "&c编辑 $chest 的奖励物品".colored())

    override fun getInventory(): Inventory {
        return inventory
    }
}