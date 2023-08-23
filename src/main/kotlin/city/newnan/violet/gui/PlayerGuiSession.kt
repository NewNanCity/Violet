package city.newnan.violet.gui

import dev.triumphteam.gui.guis.BaseGui
import me.lucko.helper.Schedulers
import org.bukkit.entity.Player

class PlayerGuiSession(val player: Player) {
    var chatInputHandlers: ((input: String) -> Boolean)? = null
    val history = ArrayDeque<Triple<BaseGui, UpdateHandler<BaseGui>?, CloseHandler<BaseGui>?>>()
    val length
        get() = history.size

    val current
        get() = history.lastOrNull()?.first

    @Synchronized
    fun <GuiType : BaseGui> open(gui: GuiType, onUpdate: UpdateHandler<GuiType>? = null, onClose: CloseHandler<GuiType>? = null) {
        if (!player.isOnline) {
            clear()
            return
        }
        history.lastOrNull()?.also { (gui, _, close) ->
            close?.invoke(CloseType.Next, gui, this)
        }
        history.addLast(Triple(gui, onUpdate as UpdateHandler<BaseGui>?, onClose as CloseHandler<BaseGui>?))
        // 原则: 不能在 close 的同一帧 open
        gui.setCloseGuiAction {
            if (current == gui) {
                history.removeLastOrNull()?.third?.invoke(CloseType.Back, gui, this)
                history.lastOrNull()?.also { (gui, update, _) ->
                    Schedulers.sync().runLater({
                        if (update?.invoke(UpdateType.Back, gui, this) == true) gui.update()
                        gui.open(player)
                    }, 1L)
                }
            }
        }
        Schedulers.sync().run {
            if (onUpdate?.invoke(UpdateType.Init, gui, this) == true) gui.update()
            gui.open(player)
        }
    }

    @Synchronized
    fun back(step: Int = 1, show: Boolean = true) {
        if (!player.isOnline) {
            clear()
            return
        }
        for (i in 1..step) {
            val (gui, _, close) = history.removeLastOrNull() ?: return
            close?.invoke(CloseType.Back, gui, this)
            if (history.isEmpty()) gui.close(player)
            if (!show && i == step) gui.close(player)
        }
        history.lastOrNull()?.also { (gui, update, _) ->
            Schedulers.sync().run {
                if (update?.invoke(UpdateType.Back, gui, this) == true) gui.update()
                if (show) gui.open(player)
            }
        }
    }

    @Synchronized
    fun refresh() {
        if (!player.isOnline) {
            clear()
            return
        }
        history.lastOrNull()?.also { (gui, update, _) ->
            Schedulers.sync().run {
                if (update?.invoke(UpdateType.Refresh, gui, this) == true) gui.update()
            }
        }
    }

    @Synchronized
    fun hide() {
        if (!player.isOnline) {
            clear()
            return
        }
        history.lastOrNull()?.also { (gui, _, close) ->
            close?.invoke(CloseType.Hide, gui, this)
            gui.close(player, false)
        }
    }

    @Synchronized
    fun show() {
        if (!player.isOnline) {
            clear()
            return
        }
        history.lastOrNull()?.also { (gui, update, _) ->
            Schedulers.sync().run {
                if (update?.invoke(UpdateType.Show, gui, this) == true) gui.update()
                gui.open(player)
            }
        }
    }

    fun clear() {
        current?.inventory?.viewers?.forEach { viewer ->
            viewer.closeInventory()
        }
        while (history.isNotEmpty()) {
            val (gui, _, close) = history.removeLast()
            close?.invoke(CloseType.Hide, gui, this)
        }
        chatInputHandlers = null
    }

    /**
     * 获取用户的下一个聊天框输入
     * @param handler 获取到输入后的回调函数，返回`true`则结束获取输入，返回`false`则继续获取输入并处理
     * @return 如果先前已经有其他输入请求，则不会开始获取输入，而返回 false，反之则返回true，开始等待输入
     */
    @Synchronized
    fun chatInput(hide: Boolean = true, handler: (input: String) -> Boolean): Boolean {
        return if (chatInputHandlers == null) {
            if (hide) hide()
            chatInputHandlers = handler
            true
        } else {
            false
        }
    }
}