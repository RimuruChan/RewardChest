package tech.rimuruchan.rewardchest.timer

import org.bukkit.entity.minecart.StorageMinecart
import org.bukkit.inventory.InventoryHolder
import org.bukkit.scheduler.BukkitRunnable
import tech.rimuruchan.rewardchest.deathChests
import tech.rimuruchan.rewardchest.gui.DeathChestGui

class DeathChestTimer(val entity: StorageMinecart, val holder: DeathChestGui) : BukkitRunnable() {

    override fun run() {
        entity.remove()
        deathChests.remove(this)
    }

    override fun cancel() {
        super.cancel()
        deathChests.remove(this)
    }
}