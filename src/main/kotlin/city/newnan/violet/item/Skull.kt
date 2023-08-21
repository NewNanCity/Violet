package city.newnan.violet.item

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.net.URL
import java.util.*

/**
 * 获取一个玩家头颅
 */
fun OfflinePlayer.getSkull(amount: Int = 1): ItemStack =
    ItemStack(Material.PLAYER_HEAD, amount).also { item ->
        item.itemMeta = (item.itemMeta as SkullMeta).also { it.owningPlayer = this }
    }

/**
 * 通过指定材质url来获取对应的头颅
 * @return 一个拥有材质的头颅
 * @throws Exception 任何异常
 */
fun URL.getSkull(amount: Int): ItemStack {
    // 创建一个头
    val item = ItemStack(Material.PLAYER_HEAD, amount)
    item.itemMeta = (item.itemMeta as SkullMeta).also {
        // 创建一个随机的UUID虚拟玩家，并赋予对应的材质
        it.ownerProfile = Bukkit.getServer().createPlayerProfile(UUID.randomUUID(), null).also { profile ->
            profile.textures.skin = this
        }
    }
    return item
}

/**
 * 通过指定材质url来获取对应的头颅
 * @return 一个拥有材质的头颅
 * @throws Exception 任何异常
 */
fun String.getSkull(amount: Int): ItemStack =
    URL(if (!startsWith("http://") && !startsWith("https://"))
        "http://textures.minecraft.net/texture/$this" else this).getSkull(amount)
