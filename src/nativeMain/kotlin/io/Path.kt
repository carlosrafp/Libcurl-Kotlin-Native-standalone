package io

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.FILENAME_MAX
import platform.posix.getcwd

/**
 *  based on https://github.com/kavanmevada/kotlin-native-io
 * **/

data class Path (internal val path: String) {

    val absolutePath = if (!path.contains(fileSaperator)) {
        memScoped {
            allocArray<ByteVar>(FILENAME_MAX).let {
                getcwd(it, FILENAME_MAX)
            }?.toKString()+fileSaperator+path
        }

    } else path

    val fileSaperator
        get() = if(kotlin.native.Platform.osFamily == OsFamily.WINDOWS) "\\" else "/"

    val nameSpace
        get() = if(kotlin.native.Platform.osFamily == OsFamily.WINDOWS) "/ " else "\\ "

    val fileName
        get() = absolutePath.substringAfterLast(fileSaperator)

    val parent: String
        get() = absolutePath.substringBeforeLast(fileSaperator)

    val resolve
        get() = absolutePath.replace(" ", nameSpace)

    fun resolve(child: String)
            = absolutePath+fileSaperator+child.replace(" ", nameSpace)
}