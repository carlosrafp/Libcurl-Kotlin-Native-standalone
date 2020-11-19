package io.streams

import io.File
import io.FileNotFoundException
import kotlinx.cinterop.*
import platform.posix.*


open class InputStream {

    private var file : File
    private var fd: CPointer<FILE>?
    private var pos:Long = 0L
    private var contentSize =  0L

    constructor(path: String) {
        this.file = File(path).apply { validate }
        fd = fopen(file.absolutePath, "rb")
        if (this.fd == null)
            throw FileNotFoundException(this.file.absolutePath, "Cannot read file")
        this.contentSize = (nativeHeap.alloc<stat>().apply { stat(file.absolutePath, this.ptr) }.st_size -1).toLong()
    }

    constructor(file: File): this(file.absolutePath)
    /*
    {
        this.file = file
        fd = fopen(file.absolutePath, "rb")
        if (this.fd == null)
            throw FileNotFoundException(this.file.absolutePath, "Cannot read file")
    }
    */

    //private var bytesRead = 0L

    private fun readBytes(bytes: ByteArray): ssize_t {
        return bytes.usePinned {
            fread(it.addressOf(0), 1, bytes.size.convert(),fd).convert()
        }
    }

    fun size(): Long = contentSize

    fun pos(): Long = pos

    fun read(buffer: ByteArray): ByteArray {
        pos += readBytes(buffer)
        //if (ret == 0L) break; /* EOF */
        //if (ret == -1L) { break; /* Handle error */ }
        return buffer
    }

    fun read(chunk: Int): ByteArray {
        val dbuff = if (contentSize-pos > chunk) chunk else (contentSize-pos).convert()
        return read(ByteArray(dbuff))
    }

    fun read() = read(1)[0]

    fun seek(n: Long):Long {
        if (fseek(this.fd, n.convert(), SEEK_SET) !=0 )
            throw FileNotFoundException(this.file.absolutePath, "fseek error")
        this.pos = n
        return n
    }

    fun available() = contentSize - pos

    fun getFD() = fd

    fun close() = fclose(fd)

    //val dbuff = if (contentSize-bytesRead > buffer.size) buffer.size.toInt() else (contentSize-bytesRead)
}