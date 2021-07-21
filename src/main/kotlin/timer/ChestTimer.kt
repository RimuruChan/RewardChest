package tech.rimuruchan.rewardchest.timer

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import tech.rimuruchan.rewardchest.*
import tech.rimuruchan.rewardchest.data.ChestData
import tech.rimuruchan.rewardchest.data.RewardChestData
import tech.rimuruchan.rewardchest.gui.ChestListGui
import java.util.*

class ChestTimer(val data: RewardChestData) : BukkitRunnable() {

    class SelectedChestData(
        var location: Location,
        var chestData: ChestData,
        var disappearRunnable: DisappearRunnable,
        var hologramEntity: ArmorStand?
    ) {
        var openedPlayer: Player? = null
        var removed = false
    }

    class DisappearRunnable(val location: Location) : BukkitRunnable() {

        override fun run() {
            location.chestTimer()?.removeChest(location)
        }

    }

    var selected: MutableMap<Location, SelectedChestData> = hashMapOf()

    override fun run() {
        selected.keys.forEach(::removeChest)
        selected.clear()
        createChest()
        ChestListGui.update()
    }

    override fun cancel() {
        super.cancel()
        selected.keys.forEach(::removeChest)
    }

    fun removeChest(location: Location) {
        val select = selected[location.savedLocation(selected.keys)]
        val block = location.block
        if (block.type == Material.CHEST) {
            val chest = block.state as Chest
            chest.blockInventory.apply {
                clear()
            }
            Bukkit.getOnlinePlayers()
                .forEach { if (it.openInventory.topInventory == (block.state as Chest).inventory) it.closeInventory() }
            block.type = Material.AIR
        }

        select?.apply {
            removed = true
            hologramEntity?.remove()
            disappearRunnable.cancel()
        }
    }

    fun createChest() {
        if (data.locations.isEmpty()) {
            return
        }
        val random = Random()
        var times = random.nextInt(data.maxGenAmount - data.minGenAmount + 1) + data.minGenAmount
        val locations = data.locations.toMutableList()
        while (times > 0) {
            times--

            if (locations.isEmpty())
                break

            //random chest
            if (data.chests.isEmpty()) {
                return
            }
            val list = arrayListOf<String>()
            data.chests.forEach { entry ->
                repeat(entry.value) {
                    list.add(entry.key)
                }
            }
            if (list.isEmpty())
                return
            list.shuffle()
            val chestData = chests[list[0]]
            chestData ?: continue

            val location = locations[random.nextInt(locations.size)]
            locations.remove(location)
            val block = location.block
            block.type = Material.CHEST
            val chest = block.state as Chest
            chest.blockInventory.apply {
                clear()
                val availableSlots = (0..26).toMutableList()
                chestData.items.forEach { itemStack ->
                    val item = itemStack.clone()
                    val chance =
                        item.itemMeta.lore?.find { lore -> lore.startsWith("chance: ") }?.substring(8)?.toIntOrNull()
                    if (chance != null && chance <= random.nextInt(100)) {
                        return@forEach
                    }
                    item.apply {
                        val meta = itemMeta
                        val lore = meta.lore ?: arrayListOf()
                        lore.removeIf { it.startsWith("chance: ") }
                        meta.lore = lore
                        itemMeta = meta
                    }
                    availableSlots.shuffle()
                    setItem(availableSlots.removeAt(0), item)
                }
            }
            if (config.getBoolean("refreshChestMessage.enable")) {
                Bukkit.broadcast(
                    config.getString(
                        "refreshChestMessage.content"
                    )
                        .replace("\$x", block.x.toString())
                        .replace("\$y", block.y.toString())
                        .replace("\$z", block.z.toString())
                        .replace("\$name", chestData.displayName)
                        .colored(), "rewardchest.broadcast.refreshchest"
                )
            }
            val disappearRunnable = DisappearRunnable(location).apply {
                runTaskLater(RewardChest.instance, chestData.disappearTime * 20L)
            }

            var hologramEntity: ArmorStand? = null

            if (config.getBoolean("holoName.enable")) {
                hologramEntity = block.location.add(0.5, -1.2, 0.5).run {
                    world.getNearbyEntities(this, 1.0, 2.0, 1.0).filterIsInstance<ArmorStand>().forEach { it.remove() }
                    world.spawnEntity(this, EntityType.ARMOR_STAND)
                } as ArmorStand
                hologramEntity.apply {
                    setGravity(false)
                    isVisible = false
                    isInvulnerable = true
                    isCustomNameVisible = true
                    customName = config.getString("holoName.content").colored()
                        .replace("\$name", chestData.displayName.colored())
                }
            }
            selected[location] = SelectedChestData(location, chestData, disappearRunnable, hologramEntity)
        }
    }
}

fun Location.chestTimer() = rchests.values.find { it.selected.keys.find { loc -> loc.block == this.block } != null }

fun Location.savedLocation(collection: Collection<Location>) = collection.find { this.block == it.block }
