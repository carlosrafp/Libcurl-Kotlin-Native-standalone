import io.File
import io.streams.InputStream
import io.streams.OutputStream
import kotlinx.cinterop.*
import kotlinx.cinterop.cValue
import libcrypto.AES_KEY
import libcrypto.AES_cbc_encrypt
import libcrypto.AES_set_decrypt_key
import libcrypto.AES_set_encrypt_key
import libcurl.*
import platform.windows.byte
import platform.windows.byteVar
import zlib.gzclose
import zlib.gzopen
import zlib.gzprintf
import kotlin.random.Random
import kotlin.random.nextUBytes


fun main() {
    val file = gzopen("teste.zip", "wb");
    val r = gzprintf(file, ", %s!", "hello")
    if (r != 8) {
        println("libz error = $r")
    }
    var site = ""
    try{
        val arq = InputStream("link.txt")
        val link = ByteArray(arq.available().toInt())
        arq.read(link)
        site = link.decodeToString()
        println("link = $link")
        arq.close()
    }
    catch (e: Exception){
        println("excecao = $e")
        val arq = OutputStream ("link.txt")
        arq.write("https://jonnyzzz.com/blog/2018/10/29/kn-libcurl-windows/".encodeToByteArray())
        site = "https://jonnyzzz.com/blog/2018/10/29/kn-libcurl-windows/"
        arq.close()
    }
    println("trying $site")
    gzclose(file)
    val AESkey = nativeHeap.alloc<AES_KEY>()
    val chave:UByteArray = Random.Default.nextUBytes(16)
    val iv:UByteArray = Random.Default.nextUBytes(16)
    //val iv2:UByteArray = UByteArray(16)
    //for (i in 0..15)
    //    iv2[i] = iv[i]
    val iv2:UByteArray = iv.toUByteArray()
    AES_set_encrypt_key(chave.refTo(0), 128, AESkey.ptr)
    val teste = "Este eh um teste"
    val teste_in = teste.encodeToByteArray()
    println("Arquivo \"teste.txt\" existe? ${File("teste.txt").exists}")
    var arq = OutputStream("teste.txt", false)
    println("arq \"${arq.filename()}\" tem ${arq.size()} bytes")
    arq.write(teste_in)
    arq.write("\r\n".encodeToByteArray())
    val teste_out = UByteArray(teste_in.size)
    AES_cbc_encrypt(teste_in.asUByteArray().refTo(0), teste_out.refTo(0), teste_out.size.convert(), AESkey.ptr, iv.refTo(0), 1)
    arq.write(teste_out.asByteArray())
    arq.write("\r\n".encodeToByteArray())
    val teste_out2 = UByteArray(teste_out.size)
    AES_set_decrypt_key(chave.refTo(0), 128, AESkey.ptr)
    AES_cbc_encrypt(teste_out.refTo(0), teste_out2.refTo(0), teste_out.size.convert(), AESkey.ptr, iv2.refTo(0), 0)
    arq.write(teste_out2.toByteArray())
    arq.write("\r\n".encodeToByteArray())
    arq.write("Este eh um teste de java".encodeToByteArray())
    println("arq \"${arq.filename()}\" tem ${arq.size()} bytes")
    arq.seek(arq.pos() - 4)
    arq.write("kotlin-native".encodeToByteArray())
    println("arq \"${arq.filename()}\" tem ${arq.size()} bytes")
    arq.close()
    nativeHeap.free(AESkey)

    //val chunk = cValue<MemoryStruct>{
    //    memory = alocarString(1)
    //    size = 1.convert()
    //}
    val chunk = retornaStruct()
    //val chunk2 = chunk.getPointer(MemScope())
    //val func = staticCFunction(WriteMemoryCallback)
    val curl = curl_easy_init()
    if (curl != null) {
        curl_easy_setopt(curl, CURLOPT_URL, site)
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L)
        curl_easy_setopt(curl, CURLOPT_FRESH_CONNECT, 1L)

        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, supply_fun());
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, chunk);

        curl_easy_setopt(curl, CURLOPT_USERAGENT, "mozilla/5.0");
        //curl_easy_setopt(curl, CURLOPT_VERBOSE, 1); // informacoes
        //curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, false) // nao usa certificado
        curl_easy_setopt(curl, CURLOPT_CAINFO, "cacert.pem"); // configura certificado
        //curl_easy_setopt(curl, CURLOPT_CAPATH, "\\.");
        val res = curl_easy_perform(curl)
        if (res != CURLE_OK) {
            println("curl_easy_perform() failed ${curl_easy_strerror(res)?.toKString()}")
        }
        //val tam = chunk?.pointed?.size?.toInt() ?: 0
        val tam = bufsize(chunk)
        val resposta = ByteArray(tam.toInt())
        copiarBuf(chunk,resposta.refTo(0))
        println("lidos ${tam.toInt()} bytes")
        arq = OutputStream("resp.txt")
        arq.write(resposta)
        arq.close()
        curl_easy_cleanup(curl)
    }
}
