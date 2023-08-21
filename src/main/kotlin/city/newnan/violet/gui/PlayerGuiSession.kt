package city.newnan.violet.gui

import dev.triumphteam.gui.guis.BaseGui
import me.lucko.helper.Schedulers
import org.bukkit.entity.Player

enum class UpdateType {
    Init, Refresh, Back, Show,
}

enum class CloseType {
    Back, Next, Hide,
}

class PlayerGuiSession(val player: Player) {
    var chatInputHandlers: ((input: String) -> Boolean)? = null
    val history = ArrayDeque<Triple<BaseGui, ((type: UpdateType) -> Boolean)?, ((type: CloseType) -> Unit)?>>()
    val length
        get() = history.size

    val current
        get() = history.lastOrNull()

    @Synchronized
    fun open(gui: BaseGui, onUpdate: ((type: UpdateType) -> Boolean)? = null, onClose: ((type: CloseType) -> Unit)?) {
        if (!player.isOnline) {
            clear()
            return
        }
        history.addLast(Triple(gui, onUpdate, onClose))
        gui.setCloseGuiAction { gui.open(it.player) }
        history.lastOrNull()?.run {
            third?.invoke(CloseType.Next)
            first.close(player, false)
        }
        Schedulers.sync().runLater({
            val refresh = onUpdate?.invoke(UpdateType.Init) ?: false
            if (refresh) gui.update()
            gui.open(player)
        }, 1)
    }

    @Synchronized
    fun back(step: Int = 1) {
        if (length <= 0) return
        for (i in 1..if (player.isOnline) step else length) {
            val last = history.removeLastOrNull() ?: return
            last.first.close(player, false)
            last.third?.invoke(CloseType.Back)
        }
        history.lastOrNull()?.also {
            Schedulers.sync().runLater({
                val refresh = it.second?.invoke(UpdateType.Back) ?: false
                if (refresh) it.first.update()
                it.first.open(player)
            }, 1)
        }
    }

    @Synchronized
    fun refresh() {
        if (!player.isOnline) {
            clear()
            return
        }
        current?.run {
            Schedulers.sync().runLater({
                val refresh = second?.invoke(UpdateType.Refresh) ?: false
                if (refresh) first.update()
                first.open(player)
            }, 1)
        }
    }

    @Synchronized
    fun hide() {
        if (!player.isOnline) {
            clear()
            return
        }
        current?.run {
            third?.invoke(CloseType.Hide)
            first.close(player, false)
        }
    }

    @Synchronized
    fun show() {
        if (!player.isOnline) {
            clear()
            return
        }
        current?.run {
            Schedulers.sync().runLater({
                val refresh = second?.invoke(UpdateType.Show) ?: false
                if (refresh) first.update()
                first.open(player)
            }, 1)
        }
    }

    fun clear() = back(length)

    /**
     * 获取用户的下一个聊天框输入
     * @param handler 获取到输入后的回调函数，返回`true`则结束获取输入，返回`false`则继续获取输入并处理
     * @return 如果先前已经有其他输入请求，则不会开始获取输入，而返回 false，反之则返回true，开始等待输入
     */
    @Synchronized
    fun chatInput(handler: (input: String) -> Boolean): Boolean {
        return if (chatInputHandlers == null) {
            chatInputHandlers = handler
            true
        } else {
            false
        }
    }
}