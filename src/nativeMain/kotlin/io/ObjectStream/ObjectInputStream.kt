package io.ObjectStream

import io.buffer.AdaptiveBuffer

class ObjectInputStream {

    private val buf: AdaptiveBuffer

    constructor(ownbuf: ByteArray) {
        buf = AdaptiveBuffer(ownbuf)
    }

    constructor(ownbuf: AdaptiveBuffer) {
        buf = ownbuf
        ownbuf.reset()
    }

    fun toByteArray(): ByteArray {
        return buf.toByteArray()
    }

    fun readBoolean(): Boolean {
        return (buf.read(1).getCharAt(0)) != 0.toChar()  // nao eh perfeito
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


}