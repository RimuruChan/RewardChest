package tech.rimuruchan.rewardchest.gui

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import tech.rimuruchan.rewardchest.chests
import tech.rimuruchan.rewardchest.colored
import tech.rimuruchan.rewardchest.config
import tech.rimuruchan.rewardchest.data.ChestData
import tech.rimuruchan.rewardchest.rchests
import tech.rimuruchan.rewardchest.timer.ChestTimer

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
        val dataMap = hashMapOf<ChestData, MutableList<ChestTimer.SelectedChestData>>()
        rchests.forEach { (_, timer) ->
            timer.selected.forEach { (_, u) ->
                var list = dataMap[u.chestData]
                list = list ?: arrayListOf()
                list.add(u)
                dataMap[u.chestData] = list
            }
        }
        chests.forEach { (_, u) ->
            val list = dataMap[u]
            list?.removeIf { it.removed }
            if (list?.find { it.openedPlayer == null } == null && !showOpened)
                return@forEach
            val item = ItemStack(Material.CHEST)
            val meta = item.itemMeta
            meta.displayName = config.getString("holoName.content").replace("\$name", u.displayName).colored()
            val refreshed = !(list == null || list.find { it.openedPlayer == null } == null)
            meta.lore = arrayListOf(
                "&6刷新状态: ${
                    run {
                        if (refreshed) {
                            "&a已刷新"
                        } else
                            "&c未刷新"
                    }
                }".colored()
            ).apply {
                if (refreshed) {
                    add("&a刷新于以下坐标".colored())
                    list?.filter { it.openedPlayer == null || showPlayer }?.forEach { data ->
                        if (showLocation)
                            add("&b坐标: &f${data.location.run { "x: $x, y: $y, z: $z" }}".colored())
                        if (showPlayer)
                            add("&a打开的玩家: &f${data.openedPlayer?.displayName ?: "暂无"}".colored())
                    }
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