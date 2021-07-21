package tech.rimuruchan.rewardchest

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import tech.rimuruchan.rewardchest.data.ChestData
import tech.rimuruchan.rewardchest.data.RewardChestData
import tech.rimuruchan.rewardchest.gui.ChestListGui
import tech.rimuruchan.rewardchest.gui.EditChestGui
import tech.rimuruchan.rewardchest.listener.PlayerListener
import tech.rimuruchan.rewardchest.timer.ChestTimer
import tech.rimuruchan.rewardchest.timer.DeathChestTimer
import java.io.File

lateinit var dataConfig: FileConfiguration
lateinit var config: FileConfiguration

var rchests: MutableMap<RewardChestData, ChestTimer> = hashMapOf()
var chests: MutableMap<String, ChestData> = hashMapOf()
var deathChests = arrayListOf<DeathChestTimer>()

@Suppress("UNCHECKED_CAST")
class RewardChest : JavaPlugin() {

    override fun onEnable() {
        // Plugin startup logic
        instance = this
        ConfigurationSerialization.registerClass(RewardChestData::class.java)
        ConfigurationSerialization.registerClass(ChestData::class.java)
        reloadConfig()
        server.pluginManager.registerEvents(PlayerListener(), this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
        rchests.values.forEach { it.cancel() }
        deathChests.iterator().forEach { it.entity.remove() }
        saveConfig()
    }

    override fun reloadConfig() {
        super.reloadConfig()
        saveDefaultConfig()
        File(dataFolder, "data.yml").createNewFile()
        tech.rimuruchan.rewardchest.config = config
        dataConfig = YamlConfiguration.loadConfiguration(File(dataFolder, "data.yml"))
        rchests.values.forEach { it.cancel() }
        chests =
            (dataConfig.getList("chests", arrayListOf<ChestData>()) as MutableList<ChestData>).associateBy { it.name }
                .toMutableMap()
        rchests =
            (dataConfig.getList("rchests", ArrayList<RewardChestData>()) as ArrayList<RewardChestData>).associate {
                it to ChestTimer(it).apply { runTaskTimer(this@RewardChest, 0L, it.refreshTime * 20L) }
            }.toMutableMap()
    }

    override fun saveConfig() {
        dataConfig.set("rchests", rchests.keys.toList())
        dataConfig.set("chests", chests.values.toList())
        dataConfig.save(File(dataFolder, "data.yml"))
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            if (sender is Player && sender.hasPermission("rewardchest.gui")) {
                sender.openInventory(ChestListGui().inventory)
                return true
            }
            "&6Powered by &bRimuruChan &av${description.version}".sendTo(sender)
            return true
        }
        if (!sender.hasPermission("rewardchest.admin")) {
            "&6Powered by &bRimuruChan &av${description.version}".sendTo(sender)
            return true
        }
        if (sender !is Player) {
            "&c你必须是一个玩家!".sendTo(sender)
            return true
        }
        try {
            when (args[0]) {
                "create" -> {
                    when (args[1]) {
                        "reward" -> {
                            args[2]
                            findChestDataByName(args[2])?.run {
                                "&c已经有一个奖励箱叫做 $name 了!".sendTo(sender)
                                return true
                            }
                            RewardChestData().apply {
                                name = args[2]
                                rchests[this] = ChestTimer(this).apply {
                                    runTaskTimer(
                                        instance,
                                        0L,
                                        refreshTime * 20L
                                    )
                                }
                            }
                            "&a新建成功".sendTo(sender)
                        }
                        "chest" -> {
                            args[2]
                            val chest = chests[args[2]]
                            sender.openInventory(EditChestGui(args[2]).inventory.apply {
                                if (chest?.items?.isNotEmpty() == true)
                                    chest.items.forEachIndexed { index, item ->
                                        setItem(index, item)
                                    }
                            })
                        }
                        else -> "&c/rchest create [reward/chest] [name]".sendTo(sender)
                    }

                }
                "delete" -> {
                    when (args[1]) {
                        "reward" -> {
                            args[2]
                            val data = findChestDataByName(args[2])
                            if (data == null) {
                                "&c${args[2]}不是一个奖励箱!".sendTo(sender)
                                return true
                            }
                            rchests[data]?.cancel()
                            rchests.remove(data)
                            "&a删除成功!".sendTo(sender)
                        }
                        "chest" -> {
                            args[2]
                            if (chests.remove(args[2]) != null) {
                                "&a删除箱子成功".sendTo(sender)
                            } else
                                "&c箱子不存在".sendTo(sender)
                        }
                        else -> "&c/rchest delete [reward/chest] [name]".sendTo(sender)
                    }
                }
                "list" -> {
                    when (args[1]) {
                        "reward" -> {
                            if (args.size == 2) {
                                "&a目前有以下奖励箱".sendTo(sender)
                                rchests.forEach { (chestData, _) ->
                                    "&b${chestData.name}".sendTo(sender)
                                }
                                return true
                            }
                            findChestDataByName(args[2])?.run {
                                "&a奖励箱 $name 的所有刷新地点".sendTo(sender)
                                locations.forEach {
                                    "&b${it.run { "x: $x, y: $y, z: $z" }}".sendTo(sender)
                                }
                                "&a奖励箱 $name 的所有会刷的箱子".sendTo(sender)
                                chests.forEach {
                                    "- &a${it.run { "$key &f的概率是 &b$value" }}".sendTo(sender)
                                }
                                return true
                            }
                            "&c这不是一个奖励箱!".sendTo(sender)
                            return true
                        }
                        "chest" -> {
                            "&a目前有以下箱子".sendTo(sender)
                            chests.forEach { (_, chestData) ->
                                "&b${chestData.name} &a- &f${chestData.displayName}".sendTo(sender)
                            }
                            return true
                        }
                        else -> "&c/rchest list [reward/chest] (name)".sendTo(sender)
                    }
                }
                "add" -> {
                    when (args[1]) {
                        "loc" -> {
                            args[2]
                            val block = sender.getTargetBlock(null, 30)
                            if (block.type != Material.CHEST) {
                                "&c请瞄准一个箱子.".sendTo(sender)
                                return true
                            }
                            val data = findChestDataByName(args[2])
                            if (data == null) {
                                "&c${args[2]}不是一个奖励箱!".sendTo(sender)
                                return true
                            }
                            val existedChestData = block.rewardChestData()
                            if (existedChestData != null) {
                                "&c目标方块已经属于奖励箱${existedChestData.name}了!".sendTo(sender)
                                return true
                            }
                            data.locations.add(block.location)
                            "&a添加成功!".sendTo(sender)
                        }
                        "chest" -> {
                            args[4]
                            val chance = args[4].toIntOrNull() ?: run {
                                "&c参数错误!".sendTo(sender)
                                return true
                            }
                            findChestDataByName(args[2])?.run {
                                chests[args[3]] = chance
                                "&a设置成功!".sendTo(sender)
                                return true
                            }
                            "&c${args[2]}不是一个奖励箱!".sendTo(sender)
                            return true
                        }
                        else -> "&c/rchest add [loc/chest] [name] (chestName)".sendTo(sender)
                    }

                }
                "remove" -> {
                    when (args[1]) {
                        "loc" -> {
                            args[5]
                            val data = findChestDataByName(args[2])
                            if (data == null) {
                                "&c${args[2]}不是一个奖励箱!".sendTo(sender)
                                return true
                            }
                            val x = args[3].toInt()
                            val y = args[4].toInt()
                            val z = args[5].toInt()
                            val location =
                                data.locations.find { it.run { x == blockX && y == blockY && z == blockZ } }
                            if (location == null) {
                                "&c目标地点不包括在该奖励箱!".sendTo(sender)
                                return true
                            }
                            data.locations.remove(location)
                            val timer = rchests[data]
                            if (timer?.selected == location)
                                timer.removeChest(location)
                            "&a删除成功!".sendTo(sender)
                        }
                        "chest" -> {
                            args[3]
                            findChestDataByName(args[2])?.run {
                                chests.remove(args[3])
                                rchests[this]?.run {
                                    selected.forEach { if (it.value.chestData.name == args[3]) removeChest(it.key) }
                                }
                                "&a移除成功!".sendTo(sender)
                                return true
                            }
                            "&c${args[2]}不是一个奖励箱!".sendTo(sender)
                            return true
                        }
                        else -> "&c/rchest remove [loc/chest] [name] ([x] [y] [z]/[chestName])".sendTo(sender)
                    }
                }
                "time" -> {
                    when (args[1]) {
                        "reward" -> {
                            if (args.size == 3) {
                                if (findChestDataByName(args[2])?.apply {
                                        "&a刷新用时: $refreshTime".sendTo(sender)
                                    } == null)
                                    "&c箱子不存在!".sendTo(sender)
                                return true
                            }
                            args[3]
                            if (findChestDataByName(args[2])?.apply {
                                    refreshTime = args[3].toIntOrNull() ?: 60
                                    "&a刷新用时设为: $refreshTime".sendTo(sender)
                                    rchests[this]?.cancel()
                                    rchests[this] = ChestTimer(this).apply {
                                        runTaskTimer(
                                            this@RewardChest, 0L,
                                            refreshTime * 20L
                                        )
                                    }
                                } == null)
                                "&c箱子不存在!".sendTo(sender)

                        }
                        "chest" -> {
                            if (args.size == 3) {
                                if (chests[args[2]]?.apply {
                                        "&a消失用时: $disappearTime".sendTo(sender)
                                    } == null)
                                    "&c箱子不存在!".sendTo(sender)
                                return true
                            }
                            args[3]
                            if (chests[args[2]]?.apply {
                                    disappearTime = args[3].toIntOrNull() ?: 40
                                    "&a消失用时设为: $disappearTime".sendTo(sender)
                                } == null)
                                "&c箱子不存在!".sendTo(sender)
                        }
                        else -> "&c/rchest time [name] [reward/chest] (refreshTime/disappearTime)".sendTo(sender)
                    }
                }
                "display" -> {
                    args[2]
                    val chest = chests[args[1]]
                    if (chest == null) {
                        "&c箱子不存在".sendTo(sender)
                        return true
                    }
                    chest.displayName = args[2]
                    "&a设置成功!".sendTo(sender)
                }
                "amount" -> {
                    when (args[1]) {
                        "min" -> {
                            args[3]
                            val chest = findChestDataByName(args[2])
                            if (chest == null) {
                                "&c奖励箱不存在".sendTo(sender)
                                return true
                            }
                            val amount = args[3].toIntOrNull() ?: 1
                            chest.minGenAmount = amount
                            "&a设置成功!".sendTo(sender)
                        }
                        "max" -> {
                            args[3]
                            val chest = findChestDataByName(args[2])
                            if (chest == null) {
                                "&c奖励箱不存在".sendTo(sender)
                                return true
                            }
                            val amount = args[3].toIntOrNull() ?: 1
                            if (amount < chest.minGenAmount) {
                                "&c不能比最小值小!".sendTo(sender)
                                return true
                            }
                            chest.maxGenAmount = amount
                            "&a设置成功!".sendTo(sender)
                        }
                        else -> {
                            args[1]
                            val chest = findChestDataByName(args[1])
                            if (chest == null) {
                                "&c奖励箱不存在".sendTo(sender)
                                return true
                            }
                            "&a奖励箱最小刷新数: ${chest.minGenAmount}, 最大刷新数: ${chest.maxGenAmount}".sendTo(sender)
                        }
                    }
                }
                "chance" -> {
                    if (args[1] == "remove")
                        sender.inventory.apply {
                            itemInMainHand = itemInMainHand.apply {
                                val meta = itemMeta
                                val lore = meta.lore ?: arrayListOf()
                                lore.removeIf { it.startsWith("chance: ") }
                                meta.lore = lore
                                itemMeta = meta
                            }
                            "&a移除概率信息成功".sendTo(sender)
                        }
                    else {
                        val chance = args[1].toIntOrNull() ?: 100
                        if (chance <= 0 || chance > 100) {
                            "&cchance必须为1-100".sendTo(sender)
                            return true
                        }
                        sender.inventory.apply {
                            itemInMainHand = itemInMainHand?.apply {
                                val meta = itemMeta
                                val lore = meta.lore ?: arrayListOf()
                                lore.removeIf { it.startsWith("chance: ") }
                                lore.add("chance: $chance")
                                meta.lore = lore
                                itemMeta = meta
                            } ?: run {
                                "&c你必须手持一个物品".sendTo(sender)
                                return true
                            }
                            "&a设置概率信息成功".sendTo(sender)
                        }
                    }
                }
                "reload" -> {
                    reloadConfig()
                    "&a配置已重载.".sendTo(sender)
                }
                "save" -> {
                    saveConfig()
                    "&a配置已保存.".sendTo(sender)
                }
                else -> {
                    "&c未知指令, 请查阅 readme.pdf".sendTo(sender)
                }
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            "&c参数错误".sendTo(sender)
        } catch (e: NumberFormatException) {
            "&c参数错误".sendTo(sender)
        }
        return true
    }

    companion object {
        lateinit var instance: RewardChest
    }
}

fun ArmorStand.deathChestTimer() = deathChests.find { it.entity == this }

fun findChestDataByName(name: String) = rchests.keys.find { it.name == name }

fun Block.rewardChestData(): RewardChestData? =
    rchests.keys.find { it.locations.find { loc -> loc == location } != null }

fun String.colored() = replace('&', '§').replace("§§", "&")

fun String.parsed(player: Player) = PlaceholderAPI.setPlaceholders(player, this)

fun String.sendTo(sender: CommandSender) = sender.sendMessage(this.colored())