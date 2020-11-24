package io.buffer

class AdaptiveBuffer {

    private var buf: ByteArray
    private var count: Int = 0
    private var len: Int = 0


    //init {
    //    buf = ByteArray(32)
    //}

    constructor() {
        buf = ByteArray(32)
        len = 32
    }

    constructor(ownbuf: ByteArray) {
        buf = ownbuf
        len = ownbuf.size
    }


    private fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, len: Int) {
        for (i in 0 until len) {
            dst[dstPos + i] = src[srcPos + i]
        }
    }


    private fun expand(i: Int) {
        if (count + i <= buf.size) {
            return
        }

        val newbuf = ByteArray((count + i) * 2)
        arraycopy(buf, 0, newbuf, 0, count)
        buf = newbuf
        len = buf.size
    }


    fun size(): Int {
        return count
    }

    fun reset() {
        count = 0
    }


    fun toByteArray(): ByteArray {
        val newArray = ByteArray(count)
        arraycopy(buf, 0, newArray, 0, count)
        return newArray
    }


    fun write(buffer: ByteArray) = write(buffer, 0, buffer.size)


    fun write(buffer: ByteArray, offset: Int, count: Int) {

        // Avoid int overflow
        if (offset < 0 || offset > buffer.size || count < 0 || count > buffer.size - offset) {
            throw IndexOutOfBoundsException()
        }

        if (count == 0) {
            return
        }

        // Expand if necessary.
        expand(count)
        arraycopy(buffer, offset, buf, this.count, count)
        this.count += count
    }

    fun writebyte(b: Byte){
        write(byteArrayOf(b),0,1)
    }

    fun read(n: Int): ByteArray{
        if (len - count < n)
            throw IndexOutOfBoundsException()
        count += n
        return buf.copyOfRange(count-4,count)
    }

}