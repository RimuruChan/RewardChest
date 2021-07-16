package tech.rimuruchan.rewardchest.gui

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import tech.rimuruchan.rewardchest.chests
import tech.rimuruchan.rewardchest.colored
import tech.rimuruchan.rewardchest.config
import tech.rimuruchan.rewardchest.schests

class ChestListGui : InventoryHolder {

    private val inventory = Bukkit.createInventory(this, 54, "&c奖励箱列表".colored())

    init {
        list.add(this)
        updateGui()
    }

    fun updateGui() {
        inventory.clear()
        val showOpened = config.getBoolean("gui.showOpened")
        val showPlayer = config.getBoolean("gui.showPlayer")
        val showLocation = config.getBoolean("gui.showLocation")
        schests.forEach { (data, timer) ->
            if (data.openedPlayer != null && !showOpened)
                return@forEach
            val item = ItemStack(Material.CHEST)
            val meta = item.itemMeta
            meta.displayName = config.getString("holoName.content").replace("\$name", data.name).colored()
            meta.lore = arrayListOf("&7这是一个随机奖励箱".colored(),
                "&6刷新状态: &f${
                    data.openedPlayer?.run { "未刷新" } ?: run {
                        if (timer.disappeared)
                            "未刷新"
                        else null
                    } ?: timer.selectedChest?.run { "已刷新: $name" } ?: "未刷新"
                }".colored()
            ).apply {
                if (showLocation) {
                    add("&b坐标: &f${data.location.run { "x: $x, y: $y, z: $z" }}".colored())
                }
                if (showPlayer) {
                    add("&a打开的玩家: &f${data.openedPlayer?.run { displayName } ?: "暂无"}".colored())
                }
            }
            item.itemMeta = meta
            inventory.addItem(item)
        }
        chests.forEach { (data, timer) ->
            if ((data.openedPlayer != null || data.locations.isEmpty()) && !showOpened)
                return@forEach
            val item = ItemStack(Material.CHEST)
            val meta = item.itemMeta
            meta.displayName = config.getString("holoName.content").replace("\$name", data.name).colored()
            meta.lore = arrayListOf(
                "&6刷新状态: &f${
                    data.openedPlayer?.run { "未刷新" } ?: run {
                        if (timer.disappeared)
                            "未刷新"
                        else null
                    } ?: run {
                        if (data.locations.isEmpty())
                            "不刷新"
                        else null
                    } ?: "已刷新"
                }".colored()
            ).apply {
                if (showLocation) {
                    add("&b坐标: &f${timer.selectedLocation?.run { "x: $x, y: $y, z: $z" } ?: "暂无"}".colored())
                }
                if (showPlayer) {
                    add("&a打开的玩家: &f${data.openedPlayer?.run { displayName } ?: "暂无"}".colored())
                }
            }
            item.itemMeta = meta
            inventory.addItem(item)
        }
    }

    override fun getInventory(): Inventory {
        return inventory
    }

    companion object {
        val list = arrayListOf<ChestListGui>()
        fun update() {
            list.removeIf { it.inventory.viewers.isEmpty() }
            list.forEach { it.updateGui() }
        }
    }
}