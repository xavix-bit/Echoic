package com.echoic.app

import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit

class DesktopWindowController(private val window: java.awt.Window) : WindowController {
    private var savedBounds: Rectangle? = null
    private var maximized = false

    override fun close() {
        window.dispose()
    }

    override fun minimize() {
        if (window is Frame) {
            window.state = Frame.ICONIFIED
        }
    }

    override fun maximize() {
        if (window is Frame) {
            if (maximized) {
                // Restore to previous bounds
                savedBounds?.let {
                    window.bounds = it
                }
                maximized = false
            } else {
                // Save current bounds, then fill screen
                savedBounds = window.bounds
                val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
                val screenBounds = screen.defaultConfiguration.bounds
                val insets = Toolkit.getDefaultToolkit().getScreenInsets(screen.defaultConfiguration)
                window.setBounds(
                    screenBounds.x + insets.left,
                    screenBounds.y + insets.top,
                    screenBounds.width - insets.left - insets.right,
                    screenBounds.height - insets.top - insets.bottom,
                )
                maximized = true
            }
        }
    }

    override fun isMaximized(): Boolean = maximized

    override fun getLocation(): Pair<Int, Int> {
        val loc = window.location
        return Pair(loc.x, loc.y)
    }

    override fun setLocation(x: Int, y: Int) {
        window.location = Point(x, y)
    }
}
