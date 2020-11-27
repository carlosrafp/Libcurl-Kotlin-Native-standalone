# libcurl kotlin-native - standalone version
kotlin native libcurl

Libcurl static 64bit library, with zlib and SSL support, compiled with mingw64.

Can be used within Kotlin-native projects to produce standalone executables with curl, openssl and zlib support (so no need to provide libcurl.dll, openssl.dll and zlib.dll files).

Also there are implementations of an Kotlin adaptive buffer and in(out)putstream for file read and write support. (based on localazy/kotlin-mpp-wininet and kavanmevada/kotlin-native-io)

Started a ObjectInput/OutputStream too, based on JVM api, still in draft
