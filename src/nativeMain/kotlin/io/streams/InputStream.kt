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
        this.file = File(path).apply { validate } // file should exist
        fd = fopen(file.absolutePath, "rb")
        if (this.fd == null)
            throw FileNotFoundException(this.file.absolutePath, "Cannot read file")
        this.contentSize = (nativeHeap.alloc<stat>().apply { stat(file.absolutePath, this.ptr) }.st_size -1).toLong()
    }

    constructor(file: File): this(file.absolutePath)

    private fun readBytes(bytes: ByteArray, len: Int): ssize_t {
        return bytes.usePinned {
            fread(it.addressOf(0), 1, len.convert(),fd).convert()
        }
    }

    fun size(): Long = contentSize

    fun pos(): Long = pos

    fun read(buffer: ByteArray): Int {
        return read(buffer,buffer.size)            //attempt to read all the buffer if available
    }

    fun read(buffer: ByteArray, chunkSize: Int): Int {   // reads a chunk from the stream to the buffer and
                                                                 // returns the num of bytes read
        var dbuff = if (this.available()  > chunkSize) chunkSize else this.available().convert()
        dbuff = readBytes(buffer,dbuff).toInt()
        pos += dbuff
        return dbuff
    }

    fun read(chunk: Int): ByteArray { // returns a chunk of the stream as ByteArray
        val dbuff = if (this.available()  > chunk) chunk else this.available().convert()
        val buffer = ByteArray(dbuff)
        read(buffer,dbuff)
        return buffer
    }

    fun readAll(): ByteArray { // returns all the stream since the beginning as ByteArray
        if (pos != 0L ) seek(0L)
        val dbuff = this.available()
        val buffer = ByteArray(dbuff.toInt())
        read(buffer,dbuff.toInt())
        return buffer
    }

    fun readRemaining(): ByteArray { // returns all the stream since the beginning as ByteArray
        val dbuff = this.available()
        val buffer = ByteArray(dbuff.toInt())
        read(buffer,dbuff.toInt())
        return buffer
    }

    fun read() = read(1)[0]  //readByte
    
    fun readByte() = read()
    
    fun seek(n: Long):Long {
        if (fseek(this.fd, n.convert(), SEEK_SET) !=0 )
            throw FileNotFoundException(this.file.absolutePath, "fseek error")
        this.pos = n
        return n
    }

    fun available() = contentSize - pos

    fun getFD() = fd

    fun close() = fclose(fd)
    
}
