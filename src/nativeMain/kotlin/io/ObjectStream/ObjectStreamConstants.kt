package io.ObjectStream

object ObjectStreamConstants {

    val TC_NULL = 0x70.toByte()
    val TC_OBJECT = 0x73.toByte()
    val TC_LONGSTRING = 0x7C.toByte()
    val TC_STRING = 0x74.toByte()
    val TC_ARRAY = 0x75.toByte()
    val TC_CLASSDESC = 0x72.toByte()

}