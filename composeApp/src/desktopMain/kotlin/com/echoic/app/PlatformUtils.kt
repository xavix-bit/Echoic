package com.echoic.app

import java.awt.Desktop
import java.io.File
import java.net.URI

actual fun openUrl(url: String) {
    try {
        // 检查是否是本地路径
        val file = File(url)
        if (file.exists()) {
            // 打开本地文件或目录
            Desktop.getDesktop().open(file)
        } else {
            // 打开 URL
            Desktop.getDesktop().browse(URI(url))
        }
    } catch (_: Exception) {
        // Fallback: try xdg-open on Linux
        try {
            Runtime.getRuntime().exec(arrayOf("xdg-open", url))
        } catch (_: Exception) {}
    }
}

actual fun echoicDataDirectory(): String =
    "${System.getProperty("user.home")}/.echoic"
