package org.btn.network


import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import org.btn.common.Log
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*
import javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier
import javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory


private val mslog = Log("HttpClientUtil")

fun httpDownloadFile(url: String, file: File): Boolean {
    var conn: HttpURLConnection? = null
    return try {
        conn = URL(url).openConnection() as HttpURLConnection
        conn.connect()
        conn.inputStream.use { input ->
            BufferedOutputStream(FileOutputStream(file)).use { output ->
                input.copyTo(output)
            }
        }
        true;
    } catch (e: Exception) {
        mslog.info("ex：$e");
        e.printStackTrace()
        false;
    } finally {
        conn?.disconnect()
    }
}

private class MyHostnameVerifier : HostnameVerifier {
    override fun verify(hostname: String?, session: SSLSession?): Boolean {
        return true
    }
}

object HttpRequestObj {
    private val log = Log("HttpRequest");

    /**
     * @return
     */
    private fun createIgnoreVerifySSL(sslVersion: String): SSLSocketFactory {
        var sc = SSLContext.getInstance(sslVersion);
        val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(
                chain: Array<java.security.cert.X509Certificate>, authType: String
            ) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate?> {
                return arrayOfNulls(0)
            }
        })

        sc!!.init(null, trustAllCerts, java.security.SecureRandom())

        // Create all-trusting host name verifier
        val allHostsValid = HostnameVerifier { _, _ -> true }
        setDefaultSSLSocketFactory(sc.socketFactory);
        setDefaultHostnameVerifier(allHostsValid);
        return sc.socketFactory;
    }

    @Throws(Exception::class)
    fun sendGetRequest(url: String, charset: String = "utf-8"): String {
        //log.info("get url:>>>$url")
        var resultBuffer = StringBuffer()
        var httpURLConnection = URL(url).openConnection() as HttpURLConnection
        if (url.toLowerCase().startsWith("https"))
            (httpURLConnection as HttpsURLConnection).sslSocketFactory = createIgnoreVerifySSL("TLS")

        httpURLConnection.setRequestProperty("Accept-Charset", charset)
        httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        if (httpURLConnection.responseCode >= 300) throw Exception("HTTP Request is not success, Response code is ${httpURLConnection.responseCode}")
        val inputStream = httpURLConnection.inputStream
        val inputStreamReader = InputStreamReader(inputStream, charset)
        val reader = BufferedReader(inputStreamReader)
        reader.use { r ->
            var temp = r.readLine()
            if (temp != null) resultBuffer.append(temp)
        }
        reader.close()
        inputStreamReader.close()
        inputStream.close()
        return resultBuffer.toString()
    }

    @Throws(Exception::class)
    fun sendPostRequest(reqBody: String, url: String, charset: String = "utf-8"): String {
        //log.info("post params:>>$reqBody;url:>>$url")
        val bufferResult = StringBuffer()
        val conn = URL(url).openConnection() as HttpsURLConnection
        if (url.toLowerCase().startsWith("https"))
            (conn as HttpsURLConnection).sslSocketFactory = createIgnoreVerifySSL("TLS")

        conn.setRequestProperty("accept", "*/*")
        conn.setRequestProperty("Content-Type", "application/json; encoding=utf-8");
        conn.setRequestProperty("connection", "Keep-Alive")
        conn.setRequestProperty(
            "user-agent",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)"
        )
        conn.doOutput = true
        conn.doInput = true

        val out = PrintWriter(OutputStreamWriter(conn.outputStream, charset))
        out.print(reqBody)
        out.flush()
        val inStream = BufferedReader(InputStreamReader(conn.inputStream, charset))
        inStream.use { r ->
            val temp = r.readLine()
            if (temp != null) bufferResult.append(temp)
        }
        out.close()
        inStream.close()

        return bufferResult.toString()
    }

    public fun getParamStr(params: Map<String, String>): String {
        val str = StringBuffer()
        var temp = ""
        params.keys.map { k ->
            temp = k + "=" + URLEncoder.encode(params[k], "utf-8") + "&"
            str.append(temp)
        }
        return str.toString().substring(0, str.toString().count() - 1)
    }

}

//SSL
var sslContext: SslContext? = null
var CERT_PSW: String? = "test"
var CERT_FILE: String? = "test"

fun getSslContext() {
    try {
        if (CERT_PSW == null || CERT_FILE == null || !File(CERT_FILE).exists()) {
            return
        }

        val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
        val keyStore = KeyStore.getInstance("JKS")
        keyStore.load(FileInputStream(CERT_FILE), CERT_PSW!!.toCharArray())
        keyManagerFactory.init(keyStore, CERT_PSW!!.toCharArray())
        sslContext = SslContextBuilder.forServer(keyManagerFactory).build()
    } catch (e: Exception) {
    }
}
