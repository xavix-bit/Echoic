package com.echoic.shared.platform

/**
 * Cross-platform file abstraction.
 * Desktop actual wraps java.io.File; other platforms provide their own.
 */
expect class PlatformFile(path: String) {
    fun exists(): Boolean
    val isDirectory: Boolean
    val isFile: Boolean
    fun length(): Long
    fun listFiles(): List<PlatformFile>?
    fun deleteRecursively()
    fun delete()
    fun mkdirs()
    fun readText(): String
    fun writeText(text: String)
    fun copyTo(target: PlatformFile, overwrite: Boolean)
    val absolutePath: String
    val canonicalPath: String
    val parentFile: PlatformFile?
    val name: String
    fun walkTopDown(): Sequence<PlatformFile>
}

expect fun PlatformFile(parent: PlatformFile, child: String): PlatformFile

expect fun platformHomeDirectory(): String
expect fun platformConfigDirectory(): String
expect fun platformCurrentTimeMillis(): Long
expect fun platformGenerateUUID(): String
expect fun platformCurrentOSName(): String
expect fun platformCreateTempFile(prefix: String, suffix: String, directory: PlatformFile): PlatformFile

/**
 * InputStream abstraction for reading files and archives.
 */
expect class PlatformInputStream {
    fun read(buffer: ByteArray): Int
    fun read(buffer: ByteArray, offset: Int, length: Int): Int
    fun read(): Int
    fun close()
    fun skip(n: Long): Long
}

expect class PlatformOutputStream {
    fun write(buffer: ByteArray, offset: Int, length: Int)
    fun write(b: Int)
    fun flush()
    fun close()
}

/**
 * Platform I/O helpers.
 */
expect fun platformFileInputStream(file: PlatformFile): PlatformInputStream
expect fun platformFileOutputStream(file: PlatformFile, append: Boolean): PlatformOutputStream
expect fun platformGzipInputStream(input: PlatformInputStream): PlatformInputStream
expect fun platformZipInputStream(input: PlatformInputStream): PlatformZipInputStream

expect class PlatformRandomAccessFile(file: PlatformFile, mode: String) {
    fun seek(pos: Long)
    fun write(buffer: ByteArray, offset: Int, length: Int)
    fun close()
}

expect class PlatformZipInputStream {
    val nextEntry: PlatformZipEntry?
    fun read(buffer: ByteArray, offset: Int, length: Int): Int
    fun read(buffer: ByteArray): Int
    fun closeEntry()
    fun close()
}

expect class PlatformZipEntry(name: String, isDirectory: Boolean) {
    val name: String
    val isDirectory: Boolean
}

/** Load a native library from the given absolute path. */
expect fun platformSystemLoad(path: String)

/** Move/rename a file atomically (like Files.move with REPLACE_EXISTING). */
expect fun platformMoveFile(source: PlatformFile, target: PlatformFile)

/** Set executable permission on a file. */
expect fun platformSetExecutable(file: PlatformFile, executable: Boolean)

/** Load resource from classpath as InputStream. */
expect fun platformGetResourceAsStream(resourcePath: String): PlatformInputStream?
