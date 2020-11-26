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
import kotlin.native.identityHashCode
import kotlin.random.Random
import kotlin.random.nextUBytes

fun main() {


/// AdaptiveBuffer and ObjectOutputStream test
    val buf = AdaptiveBuffer()
    val buf2 = ByteArray(4)
    buf2.setIntAt(0,1634951491)
    val a = ObjectOutputStream(buf)
    a.writeInt(1634951491)   // equivalente a "casa"
    //a.putShort(24899)    // equivalente a "ca"...
    //a.putShort(24947)    // equivalente a ..."sa"
    //a.putFloat(2.8059795E20F)  // equivalente a "casa"
    println("float = ${a.toByteArray().getFloatAt(0)}")
    println("int = ${a.toByteArray().getIntAt(0)}")
    val b = ObjectInputStream(buf)
    println("float = ${b.readFloat()}")
    a.writeShort(24899)
    a.writeShort(24947)
    a.writeString("\r\nEste é um teste de UTF-8!! Não sei se vai dar certo, mas coloquei bastante esforço!\r\n" +
            "Este é um teste de UTF-8!! Não sei se vai dar certo, mas coloquei bastante esforço!\r\n" +
            "Este é um teste de UTF-8!! Não sei se vai dar certo, mas coloquei bastante esforço!\r\n" +
            "Este é um teste de UTF-8!! Não sei se vai dar certo, mas coloquei bastante esforço!\r\n" +
            "Este é um teste de UTF-8!! Não sei se vai dar certo, mas coloquei bastante esforço!")
    val arq2 = OutputStream("teste2.txt")
    arq2.write(a.toByteArray())
    arq2.write("\r\n".encodeToByteArray())
    arq2.write(buf2)
    arq2.close()
    
    ////////////////////gzip test
    val file = gzopen("teste.zip", "wb");
    val r = gzprintf(file, ", %s!", "hello")
    if (r != 8) {
        println("libz error = $r")
    }
    gzclose(file)

    //// io test and openssl crypto test
    var site: String
    try{
        val arq = InputStream("link.txt")
        //val link = ByteArray(arq.available().toInt())
        //arq.read(link)
        val link = arq.readAll()
        site = link.decodeToString()
        println("link = $link")
        arq.close()
    }
    catch (e: Exception){
        println("excecao = $e")
        val arq = OutputStream ("link.txt")
        site = "https://jonnyzzz.com/blog/2018/10/29/kn-libcurl-windows/"
        arq.write(site.encodeToByteArray())
        arq.close()
    }

    val chave = nativeHeap.alloc<AES_KEY>()
    val key = Random.nextUBytes(16)
    val iv = Random.nextUBytes(16)
    val iv2 = iv.toUByteArray()  // tem que criar outro IV porque ele reescreve sobre o IV
    AES_set_encrypt_key(key.refTo(0),128,chave.ptr)

    val msg = "Este éh um teste de cbc 128 usando openssl"
    //println("msg.length = ${msg.length}")
    val msg_in = msg.encodeToByteArray()

    println("Arquivo \"teste.txt\" existe? ${File("teste.txt").exists}")
    var arq = OutputStream("teste.txt", false)
    println("arq \"${arq.filename()}\" tem ${arq.size()} bytes")
    arq.write(msg_in)
    arq.write("\r\n".encodeToByteArray())


    // encriptografa independente no numero de bytes para o mesmo numero de bytes finais, logo "resto" nao eh necessario
    //val len = msg_in.size
    //val resto = if (len % 16 > 0) 16 - (len % 16) else 16
    //val resto = 0
    //val msg_in = UByteArray(len + resto)
    //val msg_in = UByteArray(len)
    //for (i in 0 until len)
    //    msg_in[i] = msg.encodeToByteArray()[i].toUByte()
    //for (i in len until len + resto)
    //    msg_in[i] = resto.toUByte()
    //println("resto deve ser = ${msg_in[len+resto-1].toInt()}")
    val msg_out = UByteArray(msg_in.size)
    AES_cbc_encrypt(msg_in.asUByteArray().refTo(0),msg_out.refTo(0),msg_in.size.convert(),chave.ptr,iv.refTo(0), AES_ENCRYPT)

    val msg_out2 = UByteArray(msg_in.size)
    //println("resultado apos encritografar = ${msg_out.toByteArray().decodeToString()}")
    arq.write(msg_out.asByteArray())
    arq.write("\r\n".encodeToByteArray())

    AES_set_decrypt_key(key.refTo(0), 128, chave.ptr); // a chave de desencriptografia nao serve para encriptografia e o iv deve ser novo
    AES_cbc_encrypt(msg_out.refTo(0),msg_out2.refTo(0),msg_out.size.convert(),chave.ptr,iv2.refTo(0), AES_DECRYPT)
    //val len2 = msg_out2.size
    //println("chegou ate aqui,len2 = ${msg_out.size}")
    //val resto2 = msg_out2[len2-1].toInt()
    //for (i in (len2-resto2) until len2)
    //    msg_out2[i] = 0.toUByte()
    //println("resultado depois = ${msg_out2.toByteArray().toKString()}")

    arq.write(msg_out2.asByteArray())
    arq.write("\r\n".encodeToByteArray())
    arq.write("Este eh um teste de java".encodeToByteArray())
    println("arq \"${arq.filename()}\" tem ${arq.size()} bytes")
    arq.seek(arq.pos() - 4)
    arq.write("kotlin-native".encodeToByteArray())
    println("arq \"${arq.filename()}\" tem ${arq.size()} bytes")
    arq.close()
    nativeHeap.free(chave)

    //val teste = OPENSSL_hexchar2i,nt('c'.toByte().toUByte())
    //println(teste)

    //val site = "https://jonnyzzz.com/blog/2018/10/29/kn-libcurl-windows/"
    println("trying $site")

    ///////////////libcurl test

    //val chunk = retornaStruct() // struct que recebera os dados de CURL
    val chunk = nativeHeap.alloc<MemoryStruct>() // tres linhas substituem a anterior
    chunk.size = 0.convert()
    //chunk.memory = alocarString(1)
    chunk.memory = nativeHeap.allocArray<ByteVar>(1)
    //val callback = supply_fun()

    val curl = curl_easy_init()
    if (curl != null) {
        curl_easy_setopt(curl, CURLOPT_URL, site)
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L)
        //curl_easy_setopt(curl, CURLOPT_VERBOSE, 1);
        //curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, false)
        curl_easy_setopt(curl, CURLOPT_FRESH_CONNECT, 1L)

        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, supply_fun());
        //curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, callback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, chunk.ptr); // nativeHeap.alloc permite o ptr
        curl_easy_setopt(curl, CURLOPT_USERAGENT, "mozilla/5.0");

        curl_easy_setopt(curl, CURLOPT_CAINFO, "cacert.pem");
        //curl_easy_setopt(curl, CURLOPT_CAPATH, "\\.");
        val res = curl_easy_perform(curl)
        if (res != CURLE_OK) {
            println("curl_easy_perform() failed ${curl_easy_strerror(res)?.toKString()}")
        }
        //val tam = bufsize(chunk).toInt()
        val tam = chunk.size.toInt()
        val resposta = ByteArray(tam)
        copiarBuf(chunk.ptr,resposta.refTo(0))
        println("lidos ${tam.toInt()} bytes")
        arq = OutputStream("resp.txt")
        arq.write(resposta)
        arq.close()
        //liberarStruct(chunk)
        nativeHeap.free(chunk)

        curl_easy_cleanup(curl)
    }
}
