package io.ObjectStream

import io.ObjectStream.ObjectStreamConstants.TC_ARRAY
import io.ObjectStream.ObjectStreamConstants.TC_CLASSDESC
import io.ObjectStream.ObjectStreamConstants.TC_LONGSTRING
import io.ObjectStream.ObjectStreamConstants.TC_NULL
import io.ObjectStream.ObjectStreamConstants.TC_STRING
import io.buffer.AdaptiveBuffer
import kotlin.native.identityHashCode


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
        buf.writeByte((if (value) 1 else 0).toByte())
    }

    fun writeChar(value: Char) {
        buf.writeByte(value.toByte())
    }

    fun write(value: Any?){
        if (value == null){
            buf.writeByte(TC_NULL);
        }
        if (value is String) writeString(value)
        else if (value is Number){
            if (value is Float) writeFloat(value)
            else if (value is Double) writeDouble(value)
            else if (value is Char || value is Byte) buf.writeByte(value.toByte())
            else if (value is Short) writeShort(value)
            else if (value is Int) writeInt(value)
            else if (value is Long) writeLong(value)
        }
        else if (value is ByteArray) writeByteArray(value)
        // ....

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

    fun writeString(s: String) {
        val utflen: Long = getUTFLength(s)
        if (utflen <= 0xFFFF) {
            buf.writeByte(TC_STRING)
            writeUTF(s, utflen)
        } else {
            buf.writeByte(TC_LONGSTRING)
            writeLongUTF(s, utflen)
        }
    }

    fun writeByteArray(s: String) {
        buf.writeByte(TC_ARRAY) // codigo para array

        val b = s.encodeToByteArray()
        writeInt(b.size)
        buf.write(b)
    }

    fun writeBytes(b: ByteArray){
        writeInt(b.size)
        buf.write(b)
    }

    fun writeBytes(s: String){
        val b = s.encodeToByteArray()
        writeInt(b.size)
        buf.write(b)
    }

    fun writeByteArray(b: ByteArray) { // not yet fully implemented
        buf.writeByte(TC_ARRAY)
        buf.writeByte(TC_CLASSDESC);
        writeUTF("[B"); // nome da classe  - byteArray name = "[B"
        writeInt(b.identityHashCode()); // simulate UUID
        writeInt(b.contentHashCode());  // simulate UUID
        //byte flags = 0; flags |= ObjectStreamConstants.SC_SERIALIZABLE;  ==> 2
        //buf.writeByte(2);
        //writeShort(0);
        //buf.writeByte(120); // TC_ENDBLOCKDATA
        //writeNull();
        writeInt(b.size)
        buf.write(b)
    }

    fun writeNull(){
        buf.writeByte(TC_NULL);
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
            writeUTFBody(s, utflen);
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
            writeUTFBody(s, utflen);
        }
    }


}