package tech.rimuruchan.rewardchest.listener

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import tech.rimuruchan.rewardchest.*
import tech.rimuruchan.rewardchest.data.ChestData
import tech.rimuruchan.rewardchest.gui.ChestListGui
import tech.rimuruchan.rewardchest.gui.DeathChestGui
import tech.rimuruchan.rewardchest.gui.EditChestGui
import tech.rimuruchan.rewardchest.timer.DeathChestTimer
import tech.rimuruchan.rewardchest.timer.chestTimer
import tech.rimuruchan.rewardchest.timer.savedLocation
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*

val chestOpened = HashMap<Player, Block>()

@Suppress("DEPRECATION")
class PlayerListener : Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onDeathChestOpen(event: PlayerInteractAtEntityEvent) {
        event.apply {
            (rightClicked as? ArmorStand)?.deathChestTimer()?.holder?.inventory?.apply(player::openInventory)
                ?.run { isCancelled = true }
        }
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        event.apply {
            if (entity.gameMode == GameMode.CREATIVE ||
                entity.gameMode == GameMode.SPECTATOR ||
                entity.inventory.toMutableList()
                    .apply { removeIf { it == null } }.isEmpty()
            )
                return
            if (entity.world === Bukkit.getWorld(config.getString("deathChest.world"))) {
                keepInventory = true
                val armorStand = entity.world.spawnEntity(
                    entity.location.clone().add(0.0, -1.4, 0.0),
                    EntityType.ARMOR_STAND
                ) as ArmorStand
                armorStand.apply {
                    customName = config.getString("deathChest.holoName.content").parsed(entity).colored()
                    isCustomNameVisible = config.getBoolean("deathChest.holoName.enable")
                    isVisible = false
                    isInvulnerable = true
                    setGravity(false)
                    helmet = ItemStack(Material.SKULL_ITEM, 1, 0, 3)
                    Bukkit.getScheduler().runTaskAsynchronously(RewardChest.instance) {
                        var value = getHeadValue(entity.name)
                        if (value == null) {
                            value = ""
                        }
                        val item = getHead(value)
                        Bukkit.getScheduler().runTask(RewardChest.instance) {
                            armorStand.helmet = item
                        }
                    }
                }
                deathChests.add(DeathChestTimer(armorStand, DeathChestGui(entity, armorStand)).apply {
                    runTaskLater(
                        RewardChest.instance,
                        config.getInt("deathChest.time") * 20L
                    )
                })
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onChestOpen(event: PlayerInteractEvent) {
        event.apply {
            if (action == Action.RIGHT_CLICK_BLOCK &&
                clickedBlock?.type == Material.CHEST
            ) {
                var location = clickedBlock.location
                location.chestTimer()?.let {
                    location = location.savedLocation(it.selected.keys)
                    val selectedChestData = it.selected[location]
                    selectedChestData ?: return
                    if (selectedChestData.openedPlayer == null && config.getBoolean("openChestMessage.enable")) {
                        selectedChestData.openedPlayer = player
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
            block.location.chestTimer()?.run {
                Bukkit.getServer().scheduler.runTask(RewardChest.instance) {
                    block.location.savedLocation(selected.keys)?.let(::removeChest)
                }
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
                val chest = holder.chest
                val chestData = chests[chest] ?: ChestData().apply { name = chest }
                chestData.items = inventory.toMutableList().apply { removeIf { it == null } }
                chests[chest] = chestData
                "&a更新/创建成功!".sendTo(player)
                return
            } else if (holder is DeathChestGui) {
                holder.inventory.apply {
                    if (!holder.entity.isDead && toMutableList().apply { removeIf { it == null } }.isEmpty()) {
                        holder.entity.remove()
                        holder.entity.deathChestTimer()?.cancel()
                        inventory.viewers.toTypedArray().forEach { it.closeInventory() }
                    }
                }
                return
            }

            val block = chestOpened[player]
            chestOpened.remove(player)
            if (block?.type == Material.CHEST) {
                block.location.chestTimer()?.apply {
                    if ((block.state as Chest).inventory.toMutableList().apply { removeIf { it == null } }
                            .isEmpty()) {
                        block.location.savedLocation(selected.keys)?.let(::removeChest)
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun getHead(value: String): ItemStack {
    val skull = ItemStack(Material.SKULL_ITEM, 1, 0, 3)
    val hashAsId = UUID(value.hashCode().toLong(), value.hashCode().toLong())
    return Bukkit.getUnsafe().modifyItemStack(
        skull,
        "{SkullOwner:{Id:\"$hashAsId\",Properties:{textures:[{Value:\"$value\"}]}}}"
    )
}

fun getHeadValue(name: String): String? {
    try {
        val result = getURLContent("https://api.mojang.com/users/profiles/minecraft/$name")
        val g = Gson()
        var obj: JsonObject = g.fromJson(result, JsonObject::class.java)
        val uid: String = obj.get("id").toString().replace("\"", "")
        val signature = getURLContent("https://sessionserver.mojang.com/session/minecraft/profile/$uid")
        obj = g.fromJson(signature, JsonObject::class.java)
        val value: String = obj.getAsJsonArray("properties").get(0).asJsonObject.get("value").asString
        val decoded = String(Base64.getDecoder().decode(value))
        obj = g.fromJson(decoded, JsonObject::class.java)
        val skinURL: String = obj.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").asString
        val skinByte = "{\"textures\":{\"SKIN\":{\"url\":\"$skinURL\"}}}".toByteArray()
        return String(Base64.getEncoder().encode(skinByte))
    } catch (ignored: Exception) {
    }
    return null
}

private fun getURLContent(urlStr: String): String {
    val url: URL
    var reader: BufferedReader? = null
    val sb = StringBuilder()
    try {
        url = URL(urlStr)
        reader = BufferedReader(InputStreamReader(url.openStream(), StandardCharsets.UTF_8))
        var str: String?
        while (reader.readLine().also { str = it } != null) {
            sb.append(str)
        }
    } catch (ignored: Exception) {
    } finally {
        try {
            reader?.close()
        } catch (ignored: IOException) {
        }
    }
    return sb.toString()
}
