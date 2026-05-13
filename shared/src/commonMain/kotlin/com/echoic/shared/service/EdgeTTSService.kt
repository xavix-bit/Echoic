package com.echoic.shared.service

import com.echoic.shared.model.AudioFormat
import com.echoic.shared.model.TTSModel
import com.echoic.shared.model.Voice
import com.echoic.shared.platform.platformCurrentTimeMillis
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.security.MessageDigest
import kotlin.random.Random

/**
 * Edge TTS service using Microsoft's unofficial WebSocket API.
 * No API key required.
 *
 * Protocol reference: https://github.com/rany2/edge-tts
 */
class EdgeTTSService(
    private val httpClient: HttpClient,
    private val baseURL: String = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"
) : TTSService {

    companion object {
        private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        private const val CHROMIUM_VERSION = "143.0.3650.75"
        private const val SEC_MS_GEC_VERSION = "1-$CHROMIUM_VERSION"
        private const val WIN_EPOCH_SECONDS = 11644473600.0

        /** Clock skew in ms, adjusted on 403 from server Date header. */
        private var clockSkewMs: Long = 0L
    }

    override suspend fun synthesize(
        text: String,
        model: TTSModel,
        voice: Voice,
        format: AudioFormat
    ): ByteArray {
        val maxRetries = 2
        var lastException: Exception? = null

        for (attempt in 0..maxRetries) {
            try {
                return trySynthesize(text, voice)
            } catch (e: Exception) {
                lastException = e
                // Only retry on 403 handshake errors
                if (!e.message.orEmpty().contains("403")) break
            }
        }
        throw lastException ?: Exception("Edge TTS 合成失败")
    }

    private suspend fun trySynthesize(text: String, voice: Voice): ByteArray {
        val requestId = generateRequestId()
        val timestamp = generateTimestamp()
        val secMsGec = generateSecMsGec()
        val muid = generateMuid()

        val url = baseURL +
                "?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
                "&ConnectionId=$requestId" +
                "&Sec-MS-GEC=$secMsGec" +
                "&Sec-MS-GEC-Version=$SEC_MS_GEC_VERSION"

        val audioData = mutableListOf<ByteArray>()
        var errorMsg: String? = null

        httpClient.webSocket(
            urlString = url,
            request = {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0")
                header("Accept-Encoding", "gzip, deflate, br, zstd")
                header("Accept-Language", "en-US,en;q=0.9")
                header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
                header("Pragma", "no-cache")
                header("Cache-Control", "no-cache")
                header("Cookie", "muid=$muid;")
            }
        ) {
            // 1. Send speech config
            val configMsg = "X-Timestamp:$timestamp\r\n" +
                    "Content-Type:application/json; charset=utf-8\r\n" +
                    "Path:speech.config\r\n\r\n" +
                    "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"true\",\"wordBoundaryEnabled\":\"true\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}\r\n"
            send(configMsg)

            // 2. Send SSML
            val safeText = escapeXml(text)
            val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>" +
                    "<voice name='${voice.id}'><prosody pitch='+0Hz' rate='+0%' volume='+0%'>$safeText</prosody></voice></speak>"
            val ssmlMsg = "X-RequestId:$requestId\r\n" +
                    "Content-Type:application/ssml+xml\r\n" +
                    "X-Timestamp:${timestamp}Z\r\n" +
                    "Path:ssml\r\n\r\n$ssml"
            send(ssmlMsg)

            // 3. Receive messages
            try {
                while (true) {
                    val frame = incoming.receive()
                    when (frame) {
                        is Frame.Binary -> {
                            val data = frame.readBytes()
                            if (data.size > 2) {
                                val headerSize = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                                if (data.size > 2 + headerSize) {
                                    val audioChunk = data.copyOfRange(2 + headerSize, data.size)
                                    audioData.add(audioChunk)
                                }
                            }
                        }
                        is Frame.Text -> {
                            val textContent = frame.readText()
                            if (textContent.contains("Path:turn.end")) {
                                break
                            }
                        }
                        is Frame.Close -> {
                            break
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                // On 403, try to extract server Date header and adjust clock skew
                if (msg.contains("403")) {
                    adjustClockSkewFromError(msg)
                }
                errorMsg = msg
            }
        }

        if (audioData.isEmpty()) {
            val detail = errorMsg?.let { " ($it)" } ?: ""
            throw Exception("Edge TTS 合成失败：未返回音频数据$detail。请检查网络连接或稍后重试。")
        }

        // Combine all chunks
        val totalSize = audioData.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (chunk in audioData) {
            chunk.copyInto(result, offset)
            offset += chunk.size
        }
        return result
    }

    /**
     * Generate the X-Timestamp string matching edge-tts's date_to_string() format.
     * Format: "ddd MMM dd yyyy HH:mm:ss GMT+0000 (Coordinated Universal Time)"
     */
    private fun generateTimestamp(): String {
        val now = platformCurrentTimeMillis() + clockSkewMs
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = now

        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        return "${days[cal.get(Calendar.DAY_OF_WEEK) - 1]} " +
                "${months[cal.get(Calendar.MONTH)]} " +
                "${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')} " +
                "${cal.get(Calendar.YEAR)} " +
                "${cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')}:" +
                "${cal.get(Calendar.MINUTE).toString().padStart(2, '0')}:" +
                "${cal.get(Calendar.SECOND).toString().padStart(2, '0')} " +
                "GMT+0000 (Coordinated Universal Time)"
    }

    /**
     * Generate Sec-MS-GEC DRM token using Windows file time ticks.
     * Algorithm from rany2/edge-tts drm.py:
     *   ticks = (unix_seconds + WIN_EPOCH) rounded to 300s, then * 1e7
     *   hash = SHA256(f"{ticks:.0f}{TRUSTED_CLIENT_TOKEN}")
     */
    private fun generateSecMsGec(): String {
        val unixSeconds = (platformCurrentTimeMillis() + clockSkewMs) / 1000.0
        var ticks = unixSeconds + WIN_EPOCH_SECONDS
        ticks -= ticks % 300
        ticks *= 1e7 // S_TO_NS / 100

        val input = "${"%.0f".format(ticks)}$TRUSTED_CLIENT_TOKEN"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.US_ASCII))
        return hash.joinToString("") { "%02X".format(it) }
    }

    /** Generate a random MUID cookie value (32 hex chars). */
    private fun generateMuid(): String {
        return (1..32).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("").uppercase()
    }

    /**
     * On 403, try to extract the server's Date header and adjust our clock skew
     * so the next retry uses the correct timestamp.
     */
    private fun adjustClockSkewFromError(errorMsg: String) {
        // Ktor may include response headers in the exception message; try to parse a Date
        val dateRegex = Regex("""Date:\s*(.+)""", RegexOption.IGNORE_CASE)
        val match = dateRegex.find(errorMsg)
        if (match != null) {
            try {
                val serverDateStr = match.groupValues[1].trim()
                val sdf = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
                val serverTime = sdf.parse(serverDateStr)?.time ?: return
                clockSkewMs = serverTime - platformCurrentTimeMillis()
            } catch (_: Exception) {
                // Can't parse — leave skew unchanged
            }
        }
    }

    private fun generateRequestId(): String {
        val chars = "0123456789abcdef"
        return (1..32).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
