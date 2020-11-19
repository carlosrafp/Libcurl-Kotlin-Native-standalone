package io.streams

import io.File
import io.FileNotFoundException
import kotlinx.cinterop.*
import platform.posix.*


open class InputStream {

    private lateinit var file : File
    private var fd: CPointer<FILE>?

    constructor(path: String) {
        file = File(path).apply { validate }
        fd = fopen(file.absolutePath, "rb")
        if (this.fd == null)
            throw FileNotFoundException(this.file.absolutePath, "Cannot read file")
    }

    constructor(file: File): this(file.absolutePath) {
        this.file = file
        fd = fopen(file.absolutePath, "rb")
        if (this.fd == null)
            throw FileNotFoundException(this.file.absolutePath, "Cannot read file")
    }

    private var contentSize = nativeHeap.alloc<stat>().apply { stat(file.absolutePath, this.ptr) }.st_size -1

    private var bytesRead = 0L

    private fun readBytes(bytes: ByteArray): ssize_t {
        return bytes.usePinned {
            fread(it.addressOf(0), 1, bytes.size.convert(),fd).convert()
        }
    }

    fun len(): Long{
        return contentSize.toLong()
    }

    fun read(buffer: ByteArray): ByteArray {
        bytesRead += readBytes(buffer)
        //if (ret == 0L) break; /* EOF */
        //if (ret == -1L) { break; /* Handle error */ }
        return buffer
    }

    fun read(chunk: Int): ByteArray {
        val dbuff = if (contentSize-bytesRead > chunk) chunk else (contentSize-bytesRead).convert()
        return read(ByteArray(dbuff))
    }

    fun read() = read(1)[0]

    fun skip(n: Long): Long = fseek(fd, n.convert(), SEEK_SET).toLong()

    fun available() = bytesRead < contentSize

    fun getFD() = fd

    fun close() = fclose(fd)

    //val dbuff = if (contentSize-bytesRead > buffer.size) buffer.size.toInt() else (contentSize-bytesRead)
}