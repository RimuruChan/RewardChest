package tech.rimuruchan.rewardchest

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.entity.Player
import org.bukkit.entity.minecart.StorageMinecart
import org.bukkit.plugin.java.JavaPlugin
import tech.rimuruchan.rewardchest.data.ChestData
import tech.rimuruchan.rewardchest.data.SChestData
import tech.rimuruchan.rewardchest.gui.ChestListGui
import tech.rimuruchan.rewardchest.gui.EditChestGui
import tech.rimuruchan.rewardchest.listener.PlayerListener
import tech.rimuruchan.rewardchest.timer.ChestTimer
import tech.rimuruchan.rewardchest.timer.DeathChestTimer
import tech.rimuruchan.rewardchest.timer.SChestTimer
import java.io.File

lateinit var dataConfig: FileConfiguration
lateinit var config: FileConfiguration

var chests: MutableMap<ChestData, ChestTimer> = hashMapOf()
var schests: MutableMap<SChestData, SChestTimer> = hashMapOf()
var deathChests = arrayListOf<DeathChestTimer>()

@Suppress("UNCHECKED_CAST")
class RewardChest : JavaPlugin() {

    override fun onEnable() {
        // Plugin startup logic
        instance = this
        ConfigurationSerialization.registerClass(ChestData::class.java)
        ConfigurationSerialization.registerClass(SChestData::class.java)
        saveDefaultConfig()
        dataConfig = YamlConfiguration.loadConfiguration(File(dataFolder, "data.yml"))
        tech.rimuruchan.rewardchest.config = config
        File(dataFolder, "data.yml").createNewFile()
        reloadConfig()
        server.pluginManager.registerEvents(PlayerListener(), this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
        chests.values.forEach { it.cancel() }
        schests.values.forEach { it.cancel() }
        deathChests.iterator().forEach { it.entity.remove() }
        saveConfig()
    }

    override fun reloadConfig() {
        super.reloadConfig()
        tech.rimuruchan.rewardchest.config = config
        dataConfig = YamlConfiguration.loadConfiguration(File(dataFolder, "data.yml"))
        chests.values.forEach { it.cancel() }
        schests.values.forEach { it.cancel() }
        chests = (dataConfig.getList("chests", ArrayList<ChestData>()) as ArrayList<ChestData>).associate {
            it to ChestTimer(it).apply { runTaskTimer(this@RewardChest, 0L, it.refreshTime * 20L) }
        }.toMutableMap()
        schests = (dataConfig.getList("schests", ArrayList<SChestData>()) as ArrayList<SChestData>).associate {
            it to SChestTimer(it).apply { runTaskTimer(this@RewardChest, 0L, it.refreshTime * 20L) }
        }.toMutableMap()
    }

    override fun saveConfig() {
        dataConfig.set("chests", chests.keys.toList())
        dataConfig.set("schests", schests.keys.toList())
        dataConfig.save(File(dataFolder, "data.yml"))
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            if (sender is Player && sender.hasPermission("rewardchest.gui")) {
                sender.openInventory(ChestListGui().inventory)
                return true
            }
            "&6Powered by &bRimuruChan &av${description.version}".sendMessage(sender)
            return true
        }
        if (sender.hasPermission("rewardchest.admin")) {
            if (sender is Player) {
                when (args[0]) {
                    "create" -> {
                        val block = sender.getTargetBlock(null, 30)
                        if (block.type != Material.CHEST) {
                            "&c请瞄准一个箱子.".sendMessage(sender)
                            return true
                        }
                        if (args.size != 2) {
                            "&c/rchest create [name]".sendMessage(sender)
                            return true
                        }
                        block.chestData()?.run {
                            "&c目标方块已经属于奖励箱 $name 了!".sendMessage(sender)
                            return true
                        }
                        block.schestData()?.run {
                            "&c目标方块已经属于超级奖励箱 ${name} 了!".sendMessage(sender)
                            return true
                        }
                        findChestDataByName(args[1])?.run {
                            "&c已经有一个奖励箱叫做 $name 了!".sendMessage(sender)
                            return true
                        }
                        findSChestDataByName(args[1])?.run {
                            "&c已经有一个超级奖励箱叫做 $name 了!".sendMessage(sender)
                            return true
                        }

                        sender.openInventory(EditChestGui(args[1], arrayListOf()).inventory)
                    }
                    "edit" -> {
                        if (args.size != 2) {
                            "&c/rchest edit [name]".sendMessage(sender)
                            return true
                        }
                        val data = findChestDataByName(args[1])
                        if (data == null) {
                            "&c${args[1]}不是一个奖励箱!".sendMessage(sender)
                            return true
                        }
                        sender.openInventory(EditChestGui(args[1], data.locations).inventory.apply {
                            addItem(*data.items.toTypedArray())
                        })
                    }
                    "delete" -> {
                        if (args.size != 2) {
                            "&c/rchest delete [name]".sendMessage(sender)
                            return true
                        }
                        val data = findChestDataByName(args[1])
                        if (data == null) {
                            "&c${args[1]}不是一个奖励箱!".sendMessage(sender)
                            return true
                        }
                        chests[data]?.cancel()
                        chests.remove(data)
                        "&a删除成功!".sendMessage(sender)
                    }
                    "screate" -> {
                        val block = sender.getTargetBlock(null, 30)
                        if (block.type != Material.CHEST) {
                            "&c请瞄准一个箱子.".sendMessage(sender)
                            return true
                        }
                        if (args.size != 2) {
                            "&c/rchest screate [name]".sendMessage(sender)
                            return true
                        }
                        block.chestData()?.run {
                            "&c目标方块已经属于奖励箱 $name 了!".sendMessage(sender)
                            return true
                        }
                        block.schestData()?.run {
                            "&c目标方块已经属于超级奖励箱 ${name} 了!".sendMessage(sender)
                            return true
                        }
                        findChestDataByName(args[1])?.run {
                            "&c已经有一个奖励箱叫做 $name 了!".sendMessage(sender)
                            return true
                        }
                        findSChestDataByName(args[1])?.run {
                            "&c已经有一个超级奖励箱叫做 $name 了!".sendMessage(sender)
                            return true
                        }

                        SChestData(block.location).apply {
                            name = args[1]
                            schests[this] = SChestTimer(this).apply {
                                runTaskTimer(
                                    instance,
                                    0L,
                                    refreshTime * 20L
                                )
                            }
                        }
                        """
                            &a新建成功, 默认刷新时间为60s, 消失时间为40s. 使用
                            &b/rchest time [name] [refreshTime] [disappearTime]
                            &a来更改时间, 使用
                            &b/rchest sset [name] [chest] [chance]
                            &a来添加会刷新的箱子
                        """.trimIndent().sendMessage(sender)

                    }
                    "sdelete" -> {
                        if (args.size != 2) {
                            "&c/rchest sdelete [name]".sendMessage(sender)
                            return true
                        }
                        val sdata = findSChestDataByName(args[1])
                        if (sdata == null) {
                            "&c${args[1]}不是一个超级奖励箱!".sendMessage(sender)
                            return true
                        }
                        schests[sdata]?.cancel()
                        schests.remove(sdata)
                        "&a删除成功!".sendMessage(sender)
                    }
                    "list" -> {
                        when (args.size) {
                            1 -> {
                                "&a目前有以下奖励箱".sendMessage(sender)
                                chests.forEach { (chestData, _) ->
                                    "&b${chestData.name}".sendMessage(sender)
                                }
                                "&a目前有以下超级奖励箱".sendMessage(sender)
                                schests.forEach { (schestData, _) ->
                                    "&b${schestData.name} &a- &e${schestData.location.run { "x: $x, y: $y, z: $z" }}".sendMessage(
                                        sender
                                    )
                                }
                            }
                            2 -> {
                                findChestDataByName(args[1])?.run {
                                    "&a奖励箱 $name 的所有刷新地点".sendMessage(sender)
                                    locations.forEach {
                                        "&b${it.run { "x: $x, y: $y, z: $z" }}".sendMessage(sender)
                                    }
                                    return true
                                }
                                findSChestDataByName(args[1])?.run {
                                    "&a超级奖励箱 $name 的所有可刷新箱子和概率".sendMessage(sender)
                                    chests.forEach {
                                        it.run { "&b$key &a- &b$value" }.sendMessage(sender)
                                    }
                                    return true
                                }
                                "&c这不是一个奖励箱!".sendMessage(sender)
                                return true
                            }
                            else -> "&c/rchest list [name]".sendMessage(sender)
                        }
                    }
                    "add" -> {
                        val block = sender.getTargetBlock(null, 30)
                        if (block.type != Material.CHEST) {
                            "&c请瞄准一个箱子.".sendMessage(sender)
                            return true
                        }
                        if (args.size != 2) {
                            "&c/rchest add [name]".sendMessage(sender)
                            return true
                        }
                        val data = findChestDataByName(args[1])
                        if (data == null) {
                            "&c${args[1]}不是一个奖励箱!".sendMessage(sender)
                            return true
                        }
                        val badData = block.chestData()
                        if (badData != null) {
                            "&c目标方块已经属于奖励箱${badData.name}了!".sendMessage(sender)
                            return true
                        }
                        data.locations.add(block.location)
                        "&a添加成功!".sendMessage(sender)
                    }
                    "remove" -> {
                        if (args.size != 5) {
                            "&c/rchest remove [name] [x] [y] [z]".sendMessage(sender)
                            return true
                        }
                        val data = findChestDataByName(args[1])
                        if (data == null) {
                            "&c${args[1]}不是一个奖励箱!".sendMessage(sender)
                            return true
                        }
                        try {
                            val x = args[2].toInt()
                            val y = args[3].toInt()
                            val z = args[4].toInt()
                            val location = data.locations.find { it.run { x == blockX && y == blockY && z == blockZ } }
                            if (location == null) {
                                "&c目标地点不包括在该奖励箱!".sendMessage(sender)
                                return true
                            }
                            data.locations.remove(location)
                            val timer = chests[data]
                            if (timer?.selectedLocation == location)
                                timer.removeChest()
                            "&a删除成功!".sendMessage(sender)
                        } catch (e: NumberFormatException) {
                            "&c参数错误!".sendMessage(sender)
                        }
                    }
                    "time" -> {
                        if (args.size == 2) {

                            if (findChestDataByName(args[1])?.apply {
                                    "&a刷新用时: $refreshTime, 消失用时: $disappearTime".sendMessage(sender)
                                } ?: findSChestDataByName(args[1])?.apply {
                                    "&a刷新用时: $refreshTime, 消失用时: $disappearTime".sendMessage(sender)
                                } == null)
                                "&c箱子不存在!".sendMessage(sender)
                            return true
                        }
                        if (args.size != 4) {
                            "&c/rchest time [name] [refreshTime] [disappearTime]".sendMessage(sender)
                            return true
                        }
                        if (findChestDataByName(args[1])?.apply {
                                refreshTime = args[2].toIntOrNull() ?: 60
                                disappearTime = args[3].toIntOrNull() ?: 40
                                "&a刷新用时设为: $refreshTime, 消失用时: $disappearTime".sendMessage(sender)
                                chests[this]?.cancel()
                                chests[this] = ChestTimer(this).apply {
                                    runTaskTimer(
                                        this@RewardChest, 0L,
                                        refreshTime * 20L
                                    )
                                }
                            } ?: findSChestDataByName(args[1])?.apply {
                                refreshTime = args[2].toIntOrNull() ?: 60
                                disappearTime = args[3].toIntOrNull() ?: 40
                                "&a刷新用时设为: $refreshTime, 消失用时: $disappearTime".sendMessage(sender)
                                schests[this]?.cancel()
                                schests[this] = SChestTimer(this).apply {
                                    runTaskTimer(
                                        this@RewardChest, 0L,
                                        data.refreshTime * 20L
                                    )
                                }
                            } == null)
                            "&c箱子不存在!".sendMessage(sender)
                    }
                    "sset" -> {
                        if (args.size != 4) {
                            "&c/rchest sset [name] [chest] [chance]".sendMessage(sender)
                            return true
                        }
                        val chance = args[3].toIntOrNull() ?: run {
                            "&c参数错误!".sendMessage(sender)
                            return true
                        }
                        findSChestDataByName(args[1])?.run {
                            chests[args[2]] = chance
                            "&a设置成功!".sendMessage(sender)
                            return true
                        }
                        "&c${args[1]}不是一个超级奖励箱!".sendMessage(sender)
                        return true
                    }
                    "sremove" -> {
                        if (args.size != 3) {
                            "&c/rchest sremove [name] [chest]".sendMessage(sender)
                            return true
                        }
                        findSChestDataByName(args[1])?.run {
                            chests.remove(args[2])
                            schests[this]?.run {
                                if (selectedChest?.name == args[2])
                                    cancel()
                            }
                            "&a移除成功!".sendMessage(sender)
                            return true
                        }
                        "&c${args[1]}不是一个超级奖励箱!".sendMessage(sender)
                        return true
                    }
                    "reload" -> {
                        reloadConfig()
                        "&a配置已重载.".sendMessage(sender)
                    }
                    "save" -> {
                        saveConfig()
                        "&a配置已保存.".sendMessage(sender)
                    }
                    else -> {
                        "&c未知指令, 请查阅 readme.pdf".sendMessage(sender)
                    }
                }
            }
        } else "&6Powered by &bRimuruChan &av${description.version}".sendMessage(sender)
        return true
    }

    companion object {
        lateinit var instance: RewardChest
    }
}

fun StorageMinecart.deathChestTimer() = deathChests.find { it.entity == this }

fun findSChestDataByName(name: String) = schests.keys.find { it.name == name }

fun Block.schestData(): SChestData? = schests.keys.find { it.location == location }

fun findChestDataByName(name: String) = chests.keys.find { it.name == name }

fun Block.chestData(): ChestData? = chests.keys.find { it.locations.find { loc -> loc == location } != null }

fun String.colored() = replace('&', '§').replace("§§", "&")

fun String.parsed(player: Player) = PlaceholderAPI.setPlaceholders(player, this)

fun String.sendMessage(sender: CommandSender) = sender.sendMessage(this.colored())