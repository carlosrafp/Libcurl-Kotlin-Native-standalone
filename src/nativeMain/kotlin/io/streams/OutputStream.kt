package io.streams

import io.File
import io.FileNotFoundException
import kotlinx.cinterop.*
import platform.posix.*

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
        //if (append) this.fd = fopen(file.absolutePath, "ab")
        if (append && file.exists) this.fd = fopen(file.absolutePath, "r+b")
        //else if (file.exists) this.fd = fopen(file.absolutePath, "r+b")
        else this.fd = fopen(file.absolutePath, "w+b")
        //this.fd = open(file.absolutePath, O_RDWR or O_CREAT)
        if (this.fd == null)
            throw FileNotFoundException(this.file.absolutePath, "Cannot acess file")
        //this.fd = open(file.absolutePath, O_CREAT)
        if (append) {
            this.contentSize = (nativeHeap.alloc<stat>().apply { stat(file.absolutePath, this.ptr) }.st_size -1).toLong()
            seek(contentSize)
        }
        else this.pos = 0L
    }

    constructor(file: File, append: Boolean = false): this(file.absolutePath, append)

    //private var fd: Int = open(file.absolutePath, O_RDWR  or O_CREAT)
    //private var contentSize = nativeHeap.alloc<stat>().apply { stat(file.absolutePath, this.ptr) }.st_size -1

    private fun writeBytes(bytes: ByteArray): Long {
        return writeBytes(bytes,bytes.size)
    }

    private fun writeBytes(bytes: ByteArray, len: Int): Long {
        //if (offset>0) skipCurr(offset)
        return bytes.usePinned {
            val numBytes = fwrite(it.addressOf(0), 1, len.convert(),fd).toLong()
            //println ("pos = $pos, contentsize = $contentSize")
            if (pos == contentSize) contentSize += numBytes
            else if (pos + numBytes > contentSize ) contentSize = pos + numBytes
            pos += numBytes
            numBytes
        }
    }

    fun write(bytes: ByteArray) = writeBytes(bytes)
    fun write(bytes: ByteArray, len: Int) = writeBytes(bytes,len)

    fun writefromOffset(bytes: ByteArray, offset: Long): Long {
        return writefromOffset(bytes,offset,bytes.size)
    }
    fun writefromOffset(bytes: ByteArray, offset: Long, len: Int): Long {
        if (append)
            throw FileNotFoundException(this.file.absolutePath, "Cannot modify file in append mode")
        seek(offset)
        writeBytes(bytes,len)
        this.pos = contentSize
        return seek(pos)
    }

    fun seek(n: Long):Long {
        if (fseek(this.fd, n.convert(), SEEK_SET) !=0 )
            throw FileNotFoundException(this.file.absolutePath, "fseek error")
        this.pos = n
        return n
    }

    //fun seek(n: Long): Long = fseek(this.fd, n.convert(), SEEK_CUR).toLong()

    fun close() = fclose(this.fd)

    fun size() = contentSize

    fun pos() = pos

    fun filename() = file.name

    fun filepath() = file.absolutePath

    fun getFD() = this.fd
}
