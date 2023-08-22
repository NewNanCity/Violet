package city.newnan.violet.gui

enum class UpdateType {
    Init, Refresh, Back, Show,
}

enum class CloseType {
    Back, Next, Hide,
}

typealias UpdateHandler<GuiType> = (UpdateType, GuiType, PlayerGuiSession) -> Boolean
typealias CloseHandler<GuiType> = (CloseType, GuiType, PlayerGuiSession) -> Unit