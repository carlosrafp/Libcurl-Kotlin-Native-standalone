package io.buffer


/**
 *  based on https://github.com/localazy/kotlin-mpp-wininet/blob/master/src/windowsMain/kotlin/com/localazy/example/tools/AdaptiveBuffer.kt
 * **/

class AdaptiveBuffer {

    private var buf: ByteArray
    private var count: Int = 0
    private var pos: Int = 0


    constructor() { // starts an empty buffer
        buf = ByteArray(32)
    }

    constructor(ownbuf: ByteArray) {  // starts the buffer from a given ByteArray
        buf = ownbuf
        count = ownbuf.size
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
    }


    fun size(): Int {
        return count
    }
    
    fun available() = count - pos
    
    fun resetPosition() {
        pos = 0
    }
    
    fun getPos() = pos

    fun eraseAll(){
        buf = ByteArray(32)
        count = 0
        pos = 0
    }


    fun toByteArray(): ByteArray {
        //val newArray = ByteArray(count)
        //arraycopy(buf, 0, newArray, 0, count)
        //return newArray
        return buf.copyOfRange(0,count)
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

    fun writeByte(b: Byte){
        write(byteArrayOf(b),0,1)
    }

    fun writeFromDestOffset(buffer: ByteArray, DestOffset: Int, count: Int){
        writeFromDestOffset(buffer,0,DestOffset,count)
    }

    fun writeFromDestOffset(buffer: ByteArray, SrcPos:Int, DestOffset: Int, count: Int){
        if (DestOffset > this.count || DestOffset < 0 || count < 0 || count > buffer.size - SrcPos)
            throw IndexOutOfBoundsException()
        if (buffer.isEmpty() || count == 0) return // nothing to write
        val space = this.count - DestOffset
        if (count > space) expand(count - space + 1)
        arraycopy(buffer, SrcPos, buf, DestOffset, count)
        if (count > space) this.count += count - space
    }

    fun readByte(): Byte{
        if (pos >= count)
            throw IndexOutOfBoundsException()
        return buf[pos++]
    }

    fun read(n: Int): ByteArray{
        if (count - pos < n)
            throw IndexOutOfBoundsException()
        pos += n
        return buf.copyOfRange(pos-n,pos)
    }

    fun readAll(): ByteArray{
        pos = count
        return buf.copyOfRange(0,count)
    }

    fun readRemaning(): ByteArray{
        if (pos == count) return ByteArray(0)  // nothing to read
        val lastpos = pos
        pos = count
        return buf.copyOfRange(lastpos,count)
    }

    fun readFromOffset(offset: Int, count: Int): ByteArray{  // seeks from a position on buffer and changes buffer pos
        if (count > this.count - offset)
            throw IndexOutOfBoundsException()
        pos = offset + count
        if (count == 0) return ByteArray(0)         // use count 0 to repositioning of buffer pos
        return buf.copyOfRange(offset,pos)
    }

}
