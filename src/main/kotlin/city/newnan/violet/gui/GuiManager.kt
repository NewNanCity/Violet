package city.newnan.violet.gui

import me.lucko.helper.Events
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.terminable.Terminable
import me.lucko.helper.terminable.TerminableConsumer
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryInteractEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin

class GuiManager(private val plugin: Plugin) : Terminable {
    private val playerSessions = mutableMapOf<Player, PlayerGuiSession>()
    init {
        if (plugin is TerminableConsumer) bindWith(plugin)
        Events.subscribe(PlayerQuitEvent::class.java, EventPriority.MONITOR)
            .handler { playerSessions.remove(it.player)?.clear() }
            .also { if (plugin is TerminableConsumer) it.bindWith(plugin) }
        Events.subscribe(AsyncPlayerChatEvent::class.java, EventPriority.LOWEST)
            .handler { playerSessions[it.player]?.chatInputHandlers?.run {
                it.isCancelled = true
                try { if (invoke(it.message)) playerSessions[it.player]!!.chatInputHandlers = null }
                catch (e: Exception) { e.printStackTrace() }
            } }
            .also { if (plugin is TerminableConsumer) it.bindWith(plugin) }
        Events.subscribe(InventoryInteractEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.whoClicked !is Player }
            .handler {
                playerSessions[it.whoClicked as Player]?.current?.also { gui ->
                    if (gui.inventory != it.inventory) return@handler
                }
            }
    }

    override fun close() {
        playerSessions.forEach { it.value.clear() }
        playerSessions.clear()
    }

    operator fun get(player: Player): PlayerGuiSession {
        return playerSessions[player] ?: PlayerGuiSession(player).also { playerSessions[player] = it }
    }
}