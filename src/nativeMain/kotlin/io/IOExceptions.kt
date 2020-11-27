package io

/**
 *  based on https://github.com/kavanmevada/kotlin-native-io
 * **/
class FileNotFoundException(fileName: String, reason: String): Exception("FileNotFoundException: $fileName ($reason)")