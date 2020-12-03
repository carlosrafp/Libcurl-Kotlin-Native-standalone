package io.streams

import io.File
import io.FileNotFoundException
import kotlinx.cinterop.*
import platform.posix.*

/**
 *  based on https://github.com/kavanmevada/kotlin-native-io
 * **/

open class OutputStream {

    private var file : File
    private var fd: CPointer<FILE>?
    private var contentSize: Long = 0L
    private var pos:Long = 0L
    private val append:Boolean
    //private lateinit var file : File

    constructor(path: String, append: Boolean = false) {
        this.append = append
        if (append) file = File(path).apply { validate }
        else file = File(path)
        if (append && file.exists) this.fd = fopen(file.absolutePath, "r+b")
        else this.fd = fopen(file.absolutePath, "w+b")
        if (this.fd == null)
            throw FileNotFoundException(this.file.absolutePath, "Cannot acess file")
        if (append) {
            this.contentSize = (nativeHeap.alloc<stat>().apply { stat(file.absolutePath, this.ptr) }.st_size -1).toLong()
            seek(contentSize) // to simulate fopen "ab" mode, without the restriction to write only on the end of the file stream
        }
        else this.pos = 0L
    }

    constructor(file: File, append: Boolean = false): this(file.absolutePath, append)

    private fun writeBytes(bytes: ByteArray): Long {
        return writeBytes(bytes,bytes.size)
    }

    private fun writeBytes(bytes: ByteArray, len: Int): Long {
        if (len == 0) return 0L
        return bytes.usePinned {
            val numBytes = fwrite(it.addressOf(0), 1, len.convert(),fd).toLong()
            if (pos == contentSize) contentSize += numBytes
            else if (pos + numBytes > contentSize ) contentSize = pos + numBytes
            pos += numBytes
            numBytes
        }
    }

    fun write(bytes: ByteArray) = writeBytes(bytes)

    fun write(s: String) = writeBytes(s.encodeToByteArray())

    fun write(bytes: ByteArray, len: Int) = writeBytes(bytes,len)

    fun writefromOffset(bytes: ByteArray, DestOffset: Long): Long {
        return writefromOffset(bytes,0,DestOffset,bytes.size)
    }
    fun writefromOffset(bytes: ByteArray, DestOffset: Long, len: Int): Long {
        // allows writing from DestOffset even in append mode
        return writefromOffset(bytes,0,DestOffset,len)
    }

    fun writefromOffset(bytes: ByteArray, SrcOffset: Int, Destoffset: Long, len: Int): Long {
        // allows writing from DestOffset even in append mode
        if (bytes.size < len + SrcOffset)
            throw IndexOutOfBoundsException("Not enough bytes in source ByteArray")
        seek(Destoffset) // seek to file offset
        writeBytes(bytes.copyOfRange(SrcOffset,SrcOffset + len),len)
        this.pos = contentSize
        return seek(pos) // returns to the end of the stream
    }

    fun seek(n: Long):Long {
        if (n > contentSize) // cannot seek beyond file size
            throw IndexOutOfBoundsException("Cannot seek beyond file size")
        if (fseek(this.fd, n.convert(), SEEK_SET) !=0 )
            throw FileNotFoundException(this.file.absolutePath, "fseek error")
        this.pos = n
        return n
    }

    fun close() = fclose(this.fd)

    fun size() = contentSize

    fun pos() = pos

    fun filename() = file.name

    fun filepath() = file.absolutePath

    fun getFD() = this.fd
}