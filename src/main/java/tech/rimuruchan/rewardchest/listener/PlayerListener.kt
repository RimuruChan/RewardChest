package tech.rimuruchan.rewardchest.listener

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.minecart.StorageMinecart
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityInteractEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.vehicle.VehicleDestroyEvent
import tech.rimuruchan.rewardchest.*
import tech.rimuruchan.rewardchest.data.ChestData
import tech.rimuruchan.rewardchest.gui.ChestListGui
import tech.rimuruchan.rewardchest.gui.DeathChestGui
import tech.rimuruchan.rewardchest.gui.EditChestGui
import tech.rimuruchan.rewardchest.timer.ChestTimer
import tech.rimuruchan.rewardchest.timer.DeathChestTimer

val chestOpened = HashMap<Player, Block>()

class PlayerListener : Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onDeathChestOpen(event: PlayerInteractEntityEvent) {
        event.apply {
            (rightClicked as? StorageMinecart)?.deathChestTimer()?.holder?.inventory?.apply(player::openInventory)
                ?.run { isCancelled = true }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onBreakDeathChest(event: VehicleDestroyEvent){
        event.apply {
            (vehicle as? StorageMinecart)?.deathChestTimer()?.let {
                it.holder.inventory.apply {
                    contents.toMutableList().apply { removeIf { it == null } }.forEach { vehicle.world.dropItem(vehicle.location, it) }
                    clear()
                    viewers.toTypedArray().forEach { it.closeInventory() }
                    isCancelled = true
                    vehicle.remove()
                }
                it.cancel()
            }
        }
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        event.apply {
            if (entity.gameMode == GameMode.CREATIVE ||
                entity.gameMode == GameMode.SPECTATOR ||
                entity.inventory.contents.toMutableList()
                    .apply { removeIf { it == null } }.isEmpty()
            )
                return
            if (entity.world === Bukkit.getWorld(config.getString("deathChest.world"))) {
                keepInventory = true
                val minecart = entity.world.spawnEntity(entity.location, EntityType.MINECART_CHEST) as StorageMinecart
                minecart.customName = config.getString("deathChest.holoName.content").parsed(entity).colored()
                minecart.isCustomNameVisible = config.getBoolean("deathChest.holoName.enable")
                deathChests.add(DeathChestTimer(minecart, DeathChestGui(entity, minecart)).apply {
                    runTaskLater(
                        RewardChest.instance,
                        config.getInt("deathChest.time") * 20L
                    )
                })
                entity.inventory.clear()
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onChestOpen(event: PlayerInteractEvent) {
        event.apply {
            if (action == Action.RIGHT_CLICK_BLOCK &&
                clickedBlock?.type == Material.CHEST
            ) {
                clickedBlock.chestData()?.let {
                    if (it.openedPlayer == null && config.getBoolean("openChestMessage.enable")) {
                        it.openedPlayer = player
                        Bukkit.broadcast(
                            config.getString("openChestMessage.content")
                                .replace("\$x", clickedBlock.x.toString())
                                .replace("\$y", clickedBlock.y.toString())
                                .replace("\$z", clickedBlock.z.toString())
                                .parsed(player).colored(), "rewardchest.broadcast.openchest"
                        )
                        ChestListGui.update()
                    }
                    chestOpened[player] = clickedBlock
                } ?: clickedBlock.schestData()?.let {
                    if (it.openedPlayer == null && config.getBoolean("openChestMessage.enable")) {
                        it.openedPlayer = player
                        Bukkit.broadcast(
                            config.getString("openChestMessage.content")
                                .replace("\$x", clickedBlock.x.toString())
                                .replace("\$y", clickedBlock.y.toString())
                                .replace("\$z", clickedBlock.z.toString())
                                .parsed(player).colored(), "rewardchest.broadcast.openchest"
                        )
                        ChestListGui.update()
                    }
                    chestOpened[player] = clickedBlock
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onChestRemove(event: BlockBreakEvent) {
        event.apply {
            block.chestData()?.run {
                hologramEntity?.remove()
            }
        }
    }

    @EventHandler
    fun onClickInv(event: InventoryClickEvent) {
        event.apply {
            if (inventory.holder is ChestListGui) {
                isCancelled = true
            }
        }
    }

    @EventHandler
    fun onCloseInv(event: InventoryCloseEvent) {
        event.apply {
            val holder = inventory.holder
            if (holder is EditChestGui) {
                val locations = holder.locations
                findChestDataByName(holder.chest)?.apply {
                    items = inventory.contents.toMutableList().apply { removeIf { it == null } }
                    "&a更新成功!".sendMessage(player)
                } ?: run {
                    ChestData(locations).apply {
                        name = holder.chest
                        items = inventory.contents.toMutableList().apply { removeIf { it == null } }
                        refreshTime = 60
                        disappearTime = 40
                        chests[this] = ChestTimer(this).apply {
                            runTaskTimer(
                                RewardChest.instance,
                                0L,
                                refreshTime * 20L
                            )
                        }
                    }
                    """
                        &a新建成功, 默认刷新时间为60s, 消失时间为40s. 使用
                        &b/rchest time [name] [refreshTime] [disappearTime]
                        &a来更改时间, 使用
                        &b/rchest add [name]
                        &a来添加会刷新的地点
                    """.trimIndent().sendMessage(player)
                }
                return
            } else if (holder is DeathChestGui) {
                holder.inventory.apply {
                    if (!holder.entity.isDead && contents.toMutableList().apply { removeIf { it == null } }.isEmpty()) {
                        holder.entity.remove()
                        holder.entity.deathChestTimer()?.cancel()
                        inventory.viewers.toTypedArray().forEach { it.closeInventory() }
                    }
                }
            }

            val block = chestOpened[player]
            chestOpened.remove(player)
            if (block?.type == Material.CHEST) {
                block.chestData()?.apply {
                    if ((block.state as Chest).inventory.contents.toMutableList().apply { removeIf { it == null } }
                            .isEmpty()) {
                        block.type = Material.AIR
                        hologramEntity?.remove()
                    }
                } ?: block.schestData()?.apply {
                    if ((block.state as Chest).inventory.contents.toMutableList().apply { removeIf { it == null } }
                            .isEmpty()) {
                        block.type = Material.AIR
                        hologramEntity?.remove()
                    }
                }
            }
        }
    }
}