package com.echoic.shared.platform

import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

actual class PlatformFile actual constructor(path: String) {
    @PublishedApi internal val jvmFile: File = File(path)

    actual fun exists(): Boolean = jvmFile.exists()
    actual val isDirectory: Boolean get() = jvmFile.isDirectory
    actual val isFile: Boolean get() = jvmFile.isFile
    actual fun length(): Long = jvmFile.length()
    actual fun listFiles(): List<PlatformFile>? = jvmFile.listFiles()?.map { PlatformFile(it.absolutePath) }
    actual fun deleteRecursively() { jvmFile.deleteRecursively() }
    actual fun delete() { jvmFile.delete() }
    actual fun mkdirs() { jvmFile.mkdirs() }
    actual fun readText(): String = jvmFile.readText()
    actual fun writeText(text: String) { jvmFile.writeText(text) }
    actual fun copyTo(target: PlatformFile, overwrite: Boolean) { jvmFile.copyTo(target.jvmFile, overwrite) }
    actual val absolutePath: String get() = jvmFile.absolutePath
    actual val canonicalPath: String get() = jvmFile.canonicalPath
    actual val parentFile: PlatformFile? get() = jvmFile.parentFile?.let { PlatformFile(it.absolutePath) }
    actual val name: String get() = jvmFile.name
    actual fun walkTopDown(): Sequence<PlatformFile> =
        jvmFile.walkTopDown().map { PlatformFile(it.absolutePath) }
}

actual fun PlatformFile(parent: PlatformFile, child: String): PlatformFile =
    PlatformFile(File(parent.jvmFile, child).absolutePath)

actual fun platformHomeDirectory(): String = System.getProperty("user.home")

actual fun platformConfigDirectory(): String {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home")
    return when {
        os.contains("mac") -> "$home/Library/Application Support/echoic"
        os.contains("win") -> "${System.getenv("APPDATA")}/echoic"
        else -> "$home/.config/echoic"
    }
}

actual fun platformCurrentTimeMillis(): Long = System.currentTimeMillis()

@OptIn(ExperimentalUuidApi::class)
actual fun platformGenerateUUID(): String = Uuid.random().toString()

actual fun platformCurrentOSName(): String {
    val osName = System.getProperty("os.name")?.lowercase() ?: ""
    return when {
        osName.contains("mac") || osName.contains("darwin") -> "macos"
        osName.contains("win") -> "windows"
        osName.contains("linux") -> "linux"
        else -> osName
    }
}

actual fun platformCreateTempFile(prefix: String, suffix: String, directory: PlatformFile): PlatformFile {
    val f = File.createTempFile(prefix, suffix, directory.jvmFile)
    return PlatformFile(f.absolutePath)
}

// ─── InputStream / OutputStream wrappers ──────────────────────────

actual class PlatformInputStream @PublishedApi internal constructor(
    @PublishedApi internal val jvmStream: InputStream,
) {
    actual fun read(buffer: ByteArray): Int = jvmStream.read(buffer)
    actual fun read(buffer: ByteArray, offset: Int, length: Int): Int = jvmStream.read(buffer, offset, length)
    actual fun read(): Int = jvmStream.read()
    actual fun close() { jvmStream.close() }
    actual fun skip(n: Long): Long = jvmStream.skip(n)
}

actual class PlatformOutputStream @PublishedApi internal constructor(
    @PublishedApi internal val jvmStream: OutputStream,
) {
    actual fun write(buffer: ByteArray, offset: Int, length: Int) { jvmStream.write(buffer, offset, length) }
    actual fun write(b: Int) { jvmStream.write(b) }
    actual fun flush() { jvmStream.flush() }
    actual fun close() { jvmStream.close() }
}

actual fun platformFileInputStream(file: PlatformFile): PlatformInputStream =
    PlatformInputStream(BufferedInputStream(FileInputStream(file.jvmFile)))

actual fun platformFileOutputStream(file: PlatformFile, append: Boolean): PlatformOutputStream =
    PlatformOutputStream(BufferedOutputStream(FileOutputStream(file.jvmFile, append)))

actual fun platformGzipInputStream(input: PlatformInputStream): PlatformInputStream =
    PlatformInputStream(GZIPInputStream(BufferedInputStream(input.jvmStream)))

actual fun platformZipInputStream(input: PlatformInputStream): PlatformZipInputStream =
    PlatformZipInputStream(ZipInputStream(BufferedInputStream(input.jvmStream)))

actual class PlatformRandomAccessFile actual constructor(file: PlatformFile, mode: String) {
    private val raf = RandomAccessFile(file.jvmFile, mode)
    actual fun seek(pos: Long) { raf.seek(pos) }
    actual fun write(buffer: ByteArray, offset: Int, length: Int) { raf.write(buffer, offset, length) }
    actual fun close() { raf.close() }
}

actual class PlatformZipInputStream @PublishedApi internal constructor(
    @PublishedApi internal val jvmZis: ZipInputStream,
) {
    private var currentEntry: ZipEntry? = null
    actual val nextEntry: PlatformZipEntry?
        get() {
            currentEntry = jvmZis.nextEntry
            return currentEntry?.let { PlatformZipEntry(it.name, it.isDirectory) }
        }
    actual fun read(buffer: ByteArray, offset: Int, length: Int): Int = jvmZis.read(buffer, offset, length)
    actual fun read(buffer: ByteArray): Int = jvmZis.read(buffer)
    actual fun closeEntry() { jvmZis.closeEntry() }
    actual fun close() { jvmZis.close() }
}

actual class PlatformZipEntry actual constructor(
    actual val name: String,
    actual val isDirectory: Boolean,
)

actual fun platformSystemLoad(path: String) { System.load(path) }

actual fun platformMoveFile(source: PlatformFile, target: PlatformFile) {
    Files.move(source.jvmFile.toPath(), target.jvmFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
}

actual fun platformSetExecutable(file: PlatformFile, executable: Boolean) {
    file.jvmFile.setExecutable(executable)
}

actual fun platformGetResourceAsStream(resourcePath: String): PlatformInputStream? {
    val stream = PlatformFile::class.java.getResourceAsStream(resourcePath) ?: return null
    return PlatformInputStream(stream)
}
