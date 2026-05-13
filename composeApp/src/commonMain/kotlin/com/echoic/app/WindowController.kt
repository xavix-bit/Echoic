package com.echoic.app

import androidx.compose.runtime.compositionLocalOf

/**
 * Platform-agnostic window controller for commonMain.
 * Desktop implementation wraps java.awt.Window.
 */
interface WindowController {
    fun close()
    fun minimize()
    fun maximize()
    fun isMaximized(): Boolean
    fun getLocation(): Pair<Int, Int>
    fun setLocation(x: Int, y: Int)
}

val LocalWindow = compositionLocalOf<WindowController> {
    error("No WindowController provided")
}
