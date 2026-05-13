package com.echoic.app

import kotlin.math.roundToLong

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes * 10 / 1024 / 10.0} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes * 10 / (1024 * 1024) / 10.0} MB"
        else -> "${bytes * 100 / (1024 * 1024 * 1024) / 100.0} GB"
    }
}

fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
        bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024} KB/s"
        else -> "${bytesPerSecond * 10 / (1024 * 1024) / 10.0} MB/s"
    }
}

fun formatTime(seconds: Double): String {
    val mins = seconds.toLong() / 60
    val secs = seconds.toLong() % 60
    val secStr = if (secs < 10) "0$secs" else secs.toString()
    return "$mins:$secStr"
}

/** Simple placeholder replacement: replaces first %s or %d with the given arg. */
fun formatString(template: String, vararg args: Any): String {
    var result = template
    args.forEach { arg ->
        result = result.replaceFirst("%s", arg.toString())
            .replaceFirst("%d", arg.toString())
    }
    return result
}
