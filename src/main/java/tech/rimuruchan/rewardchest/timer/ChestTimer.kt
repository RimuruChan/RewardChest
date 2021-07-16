package tech.rimuruchan.rewardchest.timer

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import tech.rimuruchan.rewardchest.RewardChest
import tech.rimuruchan.rewardchest.colored
import tech.rimuruchan.rewardchest.config
import tech.rimuruchan.rewardchest.data.ChestData
import java.util.*

class ChestTimer(val data: ChestData) : BukkitRunnable() {

    var disappearTask: BukkitTask? = null
    var selectedLocation: Location? = null
    var disappeared = false

    override fun run() {
        disappearTask?.cancel()
        disappeared = false
        removeChest()
        createChest()
        val block = selectedLocation?.block ?: return
        val chest = block.state as Chest
        data.openedPlayer = null
        chest.blockInventory.apply {
            clear()
            addItem(*data.items.toTypedArray())
        }
        if (config.getBoolean("refreshChestMessage.enable")) {
            Bukkit.broadcast(
                config.getString(
                    "refreshChestMessage.content"
                )
                    .replace("\$x", block.x.toString())
                    .replace("\$y", block.y.toString())
                    .replace("\$z", block.z.toString())
                    .replace("\$name", data.name)
                    .colored(), "rewardchest.broadcast.refreshchest"
            )
        }
        disappearTask = RewardChest.instance.run {
            server.scheduler.runTaskLater(this, {
                removeChest()
                disappeared = true
            }, data.disappearTime * 20L)
        }
    }

    override fun cancel() {
        super.cancel()
        disappearTask?.cancel()
        removeChest()
    }

    fun removeChest() {
        data.hologramEntity?.remove()
        val block = selectedLocation?.block
        if (block?.type == Material.CHEST) {
            val chest = block.state as Chest
            chest.blockInventory.apply {
                clear()
            }
            Bukkit.getOnlinePlayers()
                .forEach { if (it.openInventory.topInventory == (block.state as Chest).inventory) it.closeInventory() }
            block.type = Material.AIR
        }
        selectedLocation = null
    }

    fun createChest() {
        if (data.locations.isEmpty()) {
            return
        }
        val random = Random()
        selectedLocation = data.locations[random.nextInt(data.locations.size)]
        val block = selectedLocation!!.block
        block.type = Material.CHEST
        if (config.getBoolean("holoName.enable")) {
            if (data.hologramEntity?.isDead != false) {
                data.hologramEntity = block.location.add(0.5, -1.2, 0.5).run {
                    world.getNearbyEntities(this, 0.1, 0.1, 0.1).forEach { it.remove() }
                    world.spawnEntity(this, EntityType.ARMOR_STAND)
                } as ArmorStand
                data.hologramEntity?.apply {
                    setGravity(false)
                    isVisible = false
                    isInvulnerable = true
                    isCustomNameVisible = true
                    customName = config.getString("holoName.content").colored().replace("\$name", data.name)
                }
            }
        }
    }
}