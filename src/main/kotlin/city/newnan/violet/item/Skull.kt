package city.newnan.violet.item

import city.newnan.violet.Reflection
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
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
fun URL.toSkull(amount: Int = 1): ItemStack {
    val item = ItemStack(Material.PLAYER_HEAD, amount)
    item.itemMeta = (item.itemMeta as SkullMeta).also {
        val profile = GameProfile(UUID.randomUUID(), null)
        profile.properties.put("textures", Property("textures",
            Base64.getEncoder().encodeToString("{textures:{SKIN:{url:\"$this\"}}}".toByteArray()))
        )
        Reflection.getDeclaredField(it.javaClass, "profile")?.set(it, profile)
    }
    return item
}

/**
 * 通过指定材质url来获取对应的头颅
 * @return 一个拥有材质的头颅
 * @throws Exception 任何异常
 */
fun String.toSkull(amount: Int = 1): ItemStack =
    URL(if (!startsWith("http://") && !startsWith("https://"))
        "http://textures.minecraft.net/texture/$this" else this).toSkull(amount)
