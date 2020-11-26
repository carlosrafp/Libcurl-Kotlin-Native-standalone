package io.ObjectStream

import io.buffer.AdaptiveBuffer

class ObjectInputStream {

    private val buf: AdaptiveBuffer

    constructor(ownbuf: ByteArray) {
        buf = AdaptiveBuffer(ownbuf)
    }

    constructor(ownbuf: AdaptiveBuffer) {
        buf = ownbuf
        ownbuf.resetPosition()
    }

    fun toByteArray(): ByteArray {
        return buf.toByteArray()
    }

    fun readBoolean(): Boolean {
        return (buf.read(1).getCharAt(0)) != 0.toChar()  // nao eh perfeito
    }

    fun readByte(): Byte {
        return buf.readByte()
    }

    fun readChar(): Char {
        return buf.read(1).getCharAt(0)
    }

    fun readShort(): Short {
        return buf.read(2).getShortAt(0)
    }

    fun readInt(): Int {
        return buf.read(4).getIntAt(0)
    }

    fun readFloat(): Float {
        return buf.read(4).getFloatAt(0)
    }

    fun readLong(): Long {
        return buf.read(8).getLongAt(0)
    }

    fun readDouble(): Double {
        return buf.read(8).getDoubleAt(0)
    }

    fun readBytes(len: Int): ByteArray{
        val size = readInt()
        return buf.read(size)
    }

    fun readString(): String{
        val code = readByte()
        if (code != ObjectStreamConstants.TC_STRING && code != ObjectStreamConstants.TC_LONGSTRING)
            throw Exception("illegal format, expected TC_STRING or TC_LONGSTRING flag")
        val utflen: Long
        if (code == ObjectStreamConstants.TC_STRING) utflen = readShort().toLong()
        else utflen = readLong()
        return readUTF(utflen)
    }

    fun readUTF(utflen: Long): String{
        val b = readBytes(utflen.toInt())
        return b.decodeToString()
    }

    fun readUTF(): String{
        val utflen = readShort()
        val b = readBytes(utflen.toInt())
        return b.decodeToString()
    }

    fun readLongUTF(): String{
        val utflen = readLong()
        val b = readBytes(utflen.toInt())
        return b.decodeToString()
    }

    fun readByteArray(): ByteArray{
        if (buf.readByte() != ObjectStreamConstants.TC_ARRAY && buf.readByte() != ObjectStreamConstants.TC_CLASSDESC)
            throw Exception("illegal format")
        val classname = readUTF()
        if (classname != "[B")
            throw Exception("not found Bytearray, found \"$classname\"")
        val hashId = readInt()
        val hashContent = readInt()
        val size = readInt()
        return buf.read(size)
    }

}