import kotlinx.cinterop.toKString
import libcurl.*
import zlib.gzclose
import zlib.gzopen
import zlib.gzprintf

fun main() {
    val file = gzopen("teste.zip", "wb");
    val r = gzprintf(file, ", %s!", "hello")
    if (r != 8) {
        println("libz error = $r")
    }
    val site = "https://jonnyzzz.com/blog/2018/10/29/kn-libcurl-windows/"
    println("trying $site")
    gzclose(file)
    val curl = curl_easy_init()
    if (curl != null) {
        curl_easy_setopt(curl, CURLOPT_URL, site)
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L)
        //curl_easy_setopt(curl, CURLOPT_VERBOSE, 1);
        //curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, false)
        curl_easy_setopt(curl, CURLOPT_CAINFO, "cacert.pem");
        //curl_easy_setopt(curl, CURLOPT_CAPATH, "\\.");
        val res = curl_easy_perform(curl)
        if (res != CURLE_OK) {
            println("curl_easy_perform() failed ${curl_easy_strerror(res)?.toKString()}")
        }
        curl_easy_cleanup(curl)
    }
}