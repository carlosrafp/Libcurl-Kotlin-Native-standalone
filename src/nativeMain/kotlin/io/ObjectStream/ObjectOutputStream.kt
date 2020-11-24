package io.ObjectStream

import io.buffer.AdaptiveBuffer




//import CUtils.*
//import kotlinx.cinterop.refTo

class ObjectOutputStream {

    private val buf: AdaptiveBuffer

    constructor(ownbuf: ByteArray) {
        buf = AdaptiveBuffer(ownbuf)
    }

    constructor(ownbuf: AdaptiveBuffer) {
        buf = ownbuf
    }

    fun toByteArray(): ByteArray {
        return buf.toByteArray()
    }

    fun writeBoolean(value: Boolean) {
        buf.writebyte((if (value) 1 else 0).toByte())
    }

    fun writeChar(value: Char) {
        buf.writebyte(value.toByte())
    }

    fun writeShort(value: Short) {
        val b = ByteArray(2)
        //ShortparaByteArray(value,b.refTo(0))
        b.setShortAt(0, value)
        buf.write(b)
    }

    fun writeInt(value: Int) {
        val b = ByteArray(4)
        b.setIntAt(0, value)
        //IntparaByteArray(value,b.refTo(0))
        buf.write(b)
    }

    fun writeFloat(value: Float) {
        val b = ByteArray(4)
        //FloatparaByteArray(value,b.refTo(0))
        b.setFloatAt(0, value)
        buf.write(b)
    }

    fun writeLong(value: Long) {
        val b = ByteArray(8)
        //LongparaByteArray(value,b.refTo(0))
        b.setLongAt(0, value)
        buf.write(b)
    }

    fun writeDouble(value: Double) {
        val b = ByteArray(8)
        b.setDoubleAt(0, value)
        //DoubleparaByteArray(value,b.refTo(0))
        buf.write(b)
    }

    fun getUTFLength(s: String): Long {
        val len = s.length
        var utflen: Long = 0
        var off = 0
        val cbuf: CharArray = CharArray(256)
        while (off < len) {
            val csize: Int = minOf(len - off, 256)
            s.toCharArray().copyInto(cbuf, 0, off, off + csize)
            for (cpos in 0 until csize) {
                val c: Char = cbuf.get(cpos)
                if (c.toInt() in 0x0001..0x007F) {
                    utflen++
                } else if (c.toInt() > 0x07FF) {
                    utflen += 3
                } else {
                    utflen += 2
                }
            }
            off += csize
        }
        return utflen
    }

    fun writeUTF(s: String) {
        writeUTF(s, getUTFLength(s))
    }

    fun writeBytes(s: String) {
        buf.write(s.encodeToByteArray())
    }

    fun writeBytes(b: ByteArray) {
        buf.write(b)
    }

    private fun writeUTFBody(s: String, utflen: Long) {
        val len = s.length
        var off = 0
        var pos = 0
        val cbuf= CharArray(256)
        val bbuf = ByteArray(utflen.toInt())
        while (off < len) {
            val csize: Int = minOf(len - off, 256)
            s.toCharArray().copyInto(cbuf, 0, off, off + csize)
            for (cpos in 0 until csize) {
                val c = cbuf[cpos].toInt()
                if (c <= 0x007F && c != 0) {
                    bbuf[pos++] = c.toByte()
                } else if (c.toInt() > 0x07FF) {
                    bbuf[pos + 2] = (0x80 or (c shr 0 and 0x3F)).toByte()
                    bbuf[pos + 1] = (0x80 or (c shr 6 and 0x3F)).toByte()
                    bbuf[pos + 0] = (0xE0 or (c shr 12 and 0x0F)).toByte()
                    pos += 3
                } else {
                    bbuf[pos + 1]= (0x80 or (c shr 0 and 0x3F)).toByte()
                    bbuf[pos + 0] = (0xC0 or (c shr 6 and 0x1F)).toByte()
                    pos += 2
                }
            }
            off += csize
        }
        buf.write(bbuf)
    }

    fun writeUTF(s: String, utflen: Long) {
        if (utflen > 0xFFFFL) {
            throw Exception("UTFString max length exceeded")
        }
        writeShort(utflen.toShort())
        if (utflen == s.length.toLong()) {
            writeBytes(s);
        }
        else{
            writeUTFBody(s,utflen);
        }
    }
    fun writeLongUTF(s: String) {
        writeLongUTF(s, getUTFLength(s))
    }

    fun writeLongUTF(s: String, utflen: Long) {
        writeLong(utflen)
        if (utflen == s.length.toLong()) {
            writeBytes(s);
        }
        else{
            writeUTFBody(s,utflen);
        }
    }


}