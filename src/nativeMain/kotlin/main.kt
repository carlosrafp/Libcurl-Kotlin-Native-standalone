import io.File
import io.ObjectStream.ObjectInputStream
import io.ObjectStream.ObjectOutputStream
import io.buffer.AdaptiveBuffer
import io.streams.InputStream
import io.streams.OutputStream
import kotlinx.cinterop.*
import libcurl.*
import zlib.gzclose
import zlib.gzopen
import zlib.gzprintf
import libcrypto.*
import platform.posix.free
import kotlin.random.Random
import kotlin.random.nextUBytes


fun main() {

    /// AdaptiveBuffer and ObjectOutputStream test
    val buf = AdaptiveBuffer()
    val buf2 = ByteArray(4)
    buf2.setIntAt(0,1634951491)
    val a = ObjectOutputStream(buf)
    a.writeInt(1634951491)   // same as "casa"
    //a.putShort(24899)    // same as "ca"...
    //a.putShort(24947)    // same as ..."sa"
    //a.putFloat(2.8059795E20F)  // same as "casa"
    println("float = ${a.toByteArray().getFloatAt(0)}")
    println("int = ${a.toByteArray().getIntAt(0)}")
    val b = ObjectInputStream(buf)
    println("float = ${b.readFloat()}")
    a.writeShort(24899)
    a.writeShort(24947)
    a.writeString("\r\nThis is an UTF-8 string writting test · Using special chars to really test functionality: § µ ¶\r\n" +
            "This is an UTF-8 string writting test · Using special chars to really test functionality: § µ ¶\n" +
            "This is an UTF-8 string writting test · Using special chars to really test functionality: § µ ¶\n" +
            "This is an UTF-8 string writting test · Using special chars to really test functionality: § µ ¶\n" +
            "This is an UTF-8 string writting test · Using special chars to really test functionality: § µ ¶")
    val arq2 = OutputStream("test2.txt")
    arq2.write(a.toByteArray())
    arq2.write("\r\n".encodeToByteArray())
    arq2.write(buf2)
    arq2.close()

    ////////////////////gzip test
    val file = gzopen("test.zip", "wb");
    val r = gzprintf(file, ", %s!", "hello")
    if (r != 8) {
        println("libz error = $r")
    }
    gzclose(file)

    //// io test and openssl crypto test
    var site: String
    try{
        val arq = InputStream("link.txt")
        val link = arq.readAll()
        site = link.decodeToString()
        println("link = $site")
        arq.close()
    }
    catch (e: Exception){
        println("excecao = $e")
        val arq = OutputStream ("link.txt")
        site = "https://jonnyzzz.com/blog/2018/10/29/kn-libcurl-windows/"
        arq.write(site.encodeToByteArray())
        arq.close()
    }
    val aesKey= nativeHeap.alloc<AES_KEY>()
    val key = Random.nextUBytes(16)
    val iv = Random.nextUBytes(16)
    AES_set_encrypt_key(key.refTo(0),128,aesKey.ptr)

    val msg = "This is a test of openssl® AES CBC 128 bits encryption"
    //println("msg.length = ${msg.length}")
    val msg_in = msg.encodeToByteArray()

    println("File \"test.txt\" exists? ${File("test.txt").exists}")
    var arq = OutputStream("test.txt", false)
    println("File \"${arq.filename()}\" has ${arq.size()} bytes")
    arq.write(msg_in)
    arq.write("\r\n".encodeToByteArray())

    val msg_out = UByteArray(msg_in.size)
    AES_cbc_encrypt(msg_in.asUByteArray().refTo(0),msg_out.refTo(0),msg_in.size.convert(),
        aesKey.ptr,iv.toUByteArray().refTo(0), //uses toUByteArray to generate a new UByteArray and prevents overwritting
        AES_ENCRYPT)

    val msg_out2 = UByteArray(msg_in.size)
    arq.write(msg_out.asByteArray())
    arq.write("\r\n".encodeToByteArray())

    AES_set_decrypt_key(key.refTo(0), 128, aesKey.ptr); // encrypt key doesn't works for decrypting, must create a decrypt key
    AES_cbc_encrypt(msg_out.refTo(0),msg_out2.refTo(0),msg_out.size.convert(),aesKey.ptr,iv.toUByteArray().refTo(0), AES_DECRYPT)

    arq.write(msg_out2.asByteArray())
    arq.write("\r\n".encodeToByteArray())
    arq.write("This is a test of java".encodeToByteArray())
    println("File \"${arq.filename()}\" has ${arq.size()} bytes")
    arq.seek(arq.pos() - 4)
    arq.write("kotlin-native".encodeToByteArray())
    println("File \"${arq.filename()}\" has ${arq.size()} bytes")
    arq.close()
    nativeHeap.free(aesKey)


    //val site = "https://jonnyzzz.com/blog/2018/10/29/kn-libcurl-windows/"
    println("trying $site")

    ///////////////libcurl test, using AdaptiveBuffer

    val cUrlBuf = AdaptiveBuffer()  // buffer to receive cUrl data
    val bufStableRef = StableRef.create(cUrlBuf)  // stableRef to pass CPointer to cUrl
    val callback = staticCFunction {  // callback function to retrieve data
        contents: CPointer<ByteVar>?,
        size: platform.posix.size_t,
        nmemb: platform.posix.size_t,
        userp: COpaquePointer? ->

        val numBytes = (nmemb * size) //num of byte to read
        if (userp != null && contents != null){
            val bufCurl = userp.asStableRef<AdaptiveBuffer>().get()  //convert void* to AdaptiveBuffer
            bufCurl.write(contents.readBytes(numBytes.toInt())) // readBytes from Cpointer<ByteVar>
            numBytes // num bytes written
        }
        else 0.convert()  //nothing was read
    }

    val curl = curl_easy_init()
    if (curl != null) {
        curl_easy_setopt(curl, CURLOPT_URL, site)
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L)
        //curl_easy_setopt(curl, CURLOPT_VERBOSE, 1);
        //curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, false)
        curl_easy_setopt(curl, CURLOPT_FRESH_CONNECT, 1L)

        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, callback); // callback function to write received data
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, bufStableRef.asCPointer()); // passing AdaptiveBuffer to Cfuntion
        curl_easy_setopt(curl, CURLOPT_USERAGENT, "mozilla/5.0");

        curl_easy_setopt(curl, CURLOPT_CAINFO, "cacert.pem");
        //curl_easy_setopt(curl, CURLOPT_CAPATH, "\\.");
        val res = curl_easy_perform(curl)
        if (res != CURLE_OK) {
            println("curl_easy_perform() failed ${curl_easy_strerror(res)?.toKString()}")
        }
        println("${cUrlBuf.size()} bytes read")
        val response = cUrlBuf.toByteArray()
        arq = OutputStream("resp.txt")
        arq.write(response)
        arq.close()
        bufStableRef.dispose()
        curl_easy_cleanup(curl)
    }



    /*  // same using c function calls/interop (see libcurl.def)

     //val chunk = returnStruct() // struct to receive data, can replace the next three lines
     val chunk = nativeHeap.alloc<MemoryStruct>()
     chunk.size = 0.convert()
     //chunk.memory = allocateString(1) // same as nextline
     chunk.memory = nativeHeap.allocArray<ByteVar>(1)

     val callback = supply_fun()  // using C callback function

     val curl = curl_easy_init()
     if (curl != null) {
         curl_easy_setopt(curl, CURLOPT_URL, site)
         curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L)
         //curl_easy_setopt(curl, CURLOPT_VERBOSE, 1);
         //curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, false)
         curl_easy_setopt(curl, CURLOPT_FRESH_CONNECT, 1L)

         curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, callback);
         curl_easy_setopt(curl, CURLOPT_WRITEDATA, chunk.ptr); // nativeHeap.alloc allows  o ptr
         //curl_easy_setopt(curl, CURLOPT_WRITEDATA, chunk); // if using returnStruct()
         curl_easy_setopt(curl, CURLOPT_USERAGENT, "mozilla/5.0");

         curl_easy_setopt(curl, CURLOPT_CAINFO, "cacert.pem");
         //curl_easy_setopt(curl, CURLOPT_CAPATH, "\\.");
         val res = curl_easy_perform(curl)
         if (res != CURLE_OK) {
             println("curl_easy_perform() failed ${curl_easy_strerror(res)?.toKString()}")
         }
         //val tam = bufsize(chunk).toInt()  // if using returnStruct()
         val tam = chunk.size.toInt()
         val response = ByteArray(tam)
         copyBuf(chunk.ptr,response.refTo(0))
         println("${tam.toInt()} bytes read")
         arq = OutputStream("resp.txt")
         arq.write(response)
         arq.close()
         //freeStruct(chunk)   // if using returnStruct()
         nativeHeap.free(chunk)
         curl_easy_cleanup(curl)
     }
    */

}
