package org.btn.network

//import kotlinx.coroutines.runBlocking
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelProgressiveFuture
import io.netty.channel.ChannelProgressiveFutureListener
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.*
import io.netty.handler.stream.ChunkedStream
import io.netty.util.AsciiString
import io.netty.util.concurrent.Future
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap
import io.netty.util.CharsetUtil
import io.netty.buffer.Unpooled
import org.btn.common.Log


private val mslog = Log("HttpHolderUsingContext",true)
class MsDefaultHttpResponse: DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK) {
    var isWritten = false
}

class HttpHolder {
    //private var channel: Channel? = null
    private var ctx : ChannelHandlerContext? = null
    var request: HttpRequest? = null
        private set
    private val mParams = HashMap<String, String>()
    private val mCookies = HashMap<String, String>()
    var path: String? = null
        private set
    private var response: HttpResponse? = null
    private var mStopping: Boolean = false

    val method: HttpMethod
        get() = if (request == null) {
            HttpMethod.TRACE
        } else request!!.method()

    val bodyStr: String?
        get() {
            val fullRequest = request as FullHttpRequest? ?: return null
            val content = fullRequest.content()
            if (content == null || content.readableBytes() == 0) {
                return null
            }

            val data = ByteArray(content.readableBytes())
            content.getBytes(0, data)
            try {
                return java.lang.String(data, CHARSET) as String
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }

    val params: Map<String, String>
        get() = mParams

    internal val isFree: Boolean
        get() = ctx == null



    fun getHeader(name: CharSequence): String? {
        return request!!.headers().get(name)
    }

    fun getCookie(name: String): String? {
        return mCookies[name]
    }

    fun getParam(name: String,default:String? = null): String? {
        var v = mParams[name]
        if( v == null)
            v = default
        return v
    }

    fun setRequest(ctx: ChannelHandlerContext, request: HttpRequest) {
//        println("FFFF set channel")
        mStopping = false
        this.ctx = ctx
        this.request = request
        checkMethod()
        mParams.clear()
        mCookies.clear()

        val header = request.headers().get(HttpHeaderNames.COOKIE)
        if (header != null) {
            val data = header.split("[;=]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var i = 0
            while (i < data.size - 1) {
                mCookies[data[i].trim { it <= ' ' }] = data[i + 1].trim { it <= ' ' }
                i += 2
            }
        }


        var uri = this.request!!.uri()
        val index = uri.indexOf('?')
        if (index <= 0) {
            path = uri
            return
        }
        path = uri.substring(0, index)
        uri = uri.substring(index + 1)
        val params = uri.split("[&=]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var i = 0
        while (i < params.size - 1) {
            mParams[params[i]] = params[i + 1]
            i += 2
        }
    }

    private fun checkMethod() {
        val method = request!!.method().name().toUpperCase()
        var index = method.indexOf("GET")
        if (index > 0) {
            request!!.method = HttpMethod.GET
            return
        }
        index = method.indexOf("POST")
        if (index > 0) {
            request!!.method = HttpMethod.POST
            return
        }
    }

    fun createResponse() {
        response = MsDefaultHttpResponse()
        if (HttpUtil.isKeepAlive(request!!)) {
            setHeader(HttpHeaderNames.CONNECTION, "keep-alive")
        }
    }

    fun code(code: HttpResponseStatus){
        response!!.status = code
    }
    fun code(code: Int) {
        response!!.status = HttpResponseStatus.valueOf(code)
    }

    private fun end() {
        val resp = response ?: return

        if (resp.headers()?.get(HttpHeaderNames.CONTENT_TYPE) == null) {
            setContentType("text/plain")
        }
        if (resp.headers()?.get(HttpHeaderNames.CONTENT_LENGTH) == null) {
            setHeader(HttpHeaderNames.CONTENT_LENGTH, "0")
        }

        if(resp is MsDefaultHttpResponse)
            resp.isWritten = false


        writeResponse(

        )
    }
    fun end(st: HttpResponseStatus){
        this.end(st.code())
    }
    fun end(code: Int) {
        if (mStopping || ctx == null) {
            return
        }
        code(code)
        end()
    }

    internal fun clear() {
        request = null
        path = null
        response = null
        ctx = null
        mParams.clear()
        mCookies.clear()
    }

    fun sendString(str: String, contentType:String = "text/plain;charset=utf-8") :Boolean{
        return sendStringBytes(str.toByteArray(),contentType)
    }

    fun sendStringBytes(strBytes: ByteArray,contentType:String = "text/plain;charset=utf-8"):Boolean {
        return try {
            setContentType(contentType)
            val origin = request?.headers()?.get(HttpHeaderNames.ORIGIN)
            true
        } catch (e: UnsupportedEncodingException) {
            end(500)
            false
        }
    }


    fun setHeader(name: CharSequence, value: Any) {
        response!!.headers().set(name, value)
    }

    fun addHeader(name: CharSequence, value: Any) {
        response!!.headers().add(name, value)
    }

    fun setContentType(type: String) {
        setHeader(HttpHeaderNames.CONTENT_TYPE, type)
    }

    fun sendFile(file: File){
        if(!file.exists()){
            code(HttpResponseStatus.NOT_FOUND)
            end()
            return
        }
        if (response!!.headers().get(HttpHeaderNames.CONTENT_TYPE) == null) {
            setContentType(getContentType(file.absolutePath))
        }
        try {
            val fis = FileInputStream(file)
            sendStream(fis, file.length())
        } catch (e: Exception) {
        }

    }

    private val maxPartSize = 1024*1024*8

    private suspend fun sendStreamImplAsync(istream: InputStream, totalLength: Long){
        val ctx = this.ctx
                ?: return
        val sw = Log("SendStreamImpl",true)
        var start: Long = 0
        var end = totalLength - 1

        val range = getHeader(HttpHeaderNames.RANGE)
        if (range != null) {
            sw.err("FFFF parsing range thread id="+Thread.currentThread().id)

            var parts = range.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size != 2 || "bytes" != parts[0]) {
                end(HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE.code())
                return
            }

            parts = parts[1].split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            try {
                if (parts.size > 2)
                    throw RuntimeException("error range")
                if (parts[0].isNotEmpty())
                    start = java.lang.Long.parseLong(parts[0])

                end = if (parts.size == 2 && parts[1].isNotEmpty())
                    java.lang.Long.parseLong(parts[1])
                else
                    totalLength - 1

//                if(end > start + maxPartSize)
//                    end = start + maxPartSize

                code(HttpResponseStatus.PARTIAL_CONTENT.code())
                setHeader(
                        HttpHeaderNames.CONTENT_RANGE,
                        String.format(Locale.ENGLISH, "bytes %d-%d/%d", start, end, totalLength))

                sw.log("FFFF parsing range successful total=$totalLength start=$start end=$end")
            } catch (e: Exception) {
                end(HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE.code())
                return
            }

        }

        if (mStopping)
            return

        try {
            if (start != istream.skip(start)) {
                sw.log("FFFF skip failed to skip requested amount")
                return
            }
            sw.log("FFFF after skip val=$start")
            val len = end - start + 1
            setHeader(HttpHeaderNames.CONTENT_LENGTH, len.toString())
            setHeader(HttpHeaderNames.ACCEPT_RANGES, "bytes")
//            setHeader(TRANSFER_ENCODING, CHUNKED)


            val ch = EmbeddedChannel(HttpResponseEncoder())
            ch.writeOutbound(response)
            val encoded = ch.readOutbound<ByteBuf>()
            mslog.info("response header size=${encoded.readableBytes()}")
            if(writeResponse())
                return

            val channel = ctx.channel()

            mslog.info("end=$end start=$start len=$len")
            val chunkStream = ChunkedStream(LimitInputStream(istream, len, sw.silent))


            channel.writeAndFlush(chunkStream, channel.newProgressivePromise()).addListener(object:ChannelProgressiveFutureListener{
                override fun operationComplete(future: ChannelProgressiveFuture?) {
                    mslog.info("Close file stream")
                    istream.close()
                    channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                }
                override fun operationProgressed(future: ChannelProgressiveFuture?, progress: Long, total: Long) {
                    mslog.info("write file progress $progress/$total")
                }
            })
            return
        } catch (e: Exception) {
            val swr = PrintWriter(StringWriter())
            e.printStackTrace(swr)
            sw.err("FFFF read stream exception"+e.message,e)
            e.printStackTrace()
            end(500)
            mslog.info("Close file stream")
            istream.close()
            return
        }
    }

    fun writeResponse():Boolean{
        return true

    }
    fun sendStream(istream: InputStream, totalLength: Long)  {
        runBlocking {
            mslog.info("before call sendStreamImplAsync")
            val r = sendStreamImplAsync(istream,totalLength)
            mslog.info("after call sendStreamImplAsync. result=$r")
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (request != null) {
            sb.append(",method=")
            sb.append(request!!.method().name())
            sb.append(",req=")
            sb.append(request!!.uri())
        }
        if (response != null) {
            sb.append(",res=")
            sb.append(response!!.status().code())
        }
        sb.insert(0, '[')
        sb.append(']')
        return sb.toString()
    }

    fun sendJsonCode(code: Int, desc: String? = ""):Boolean {
        var s = """{"code":$code,"desc":"$desc"}"""
        return this.sendString(s)
    }

    fun retain() {
        val req =  request
        if(req is FullHttpRequest){
            req.retain()
        }
    }
    fun release(){
        val req =  request
        if(req is FullHttpRequest){
            req.release()
        }
    }
}

class MsFullHttpResponse:HttpResponseWrapper{
    constructor():this(DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
    constructor(ver:HttpVersion,status:HttpResponseStatus):this(DefaultFullHttpResponse(ver,status))
    constructor(resp:FullHttpResponse):super(resp)




    fun writeAndFlush(ctx: ChannelHandlerContext) {
        if(content == ""){
            setHeader(HttpHeaderNames.CONTENT_LENGTH,0)

        }else{
            val buf = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
            (response as FullHttpResponse).content().writeBytes(buf)
            buf.release()
        }
        ctx.writeAndFlush(response)
    }
    fun setJsonCode(code:Int,desc:String? = ""){
        setHeader(HttpHeaderNames.CONTENT_TYPE,"application/json")
        content = """{"code":$code,"desc":"$desc"}"""
    }


    var content:String = ""
}
open class HttpResponseWrapper(var response:HttpResponse){
    var statusCode:HttpResponseStatus?
        set(value) {
            response.status = value
        }
        get() = response.status()

    fun setHeader(name:AsciiString,value:Any) { response.headers()[name] = value}
    fun addHeader(name: AsciiString?, value: String) = response.headers().add(name,value)
    fun getHeader(name: AsciiString) = response.headers()[name]
}
class HttpRequestWrapper(var request:HttpRequest){
    private val mParams = ConcurrentHashMap<String,String>()
    private val mCookies =  ConcurrentHashMap<String,String>()
    private val path:String

    init {
        val header = request.headers().get(HttpHeaderNames.COOKIE)
        if (header != null) {
            val data = header.split("[;=]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var i = 0
            while (i < data.size - 1) {
                mCookies[data[i].trim { it <= ' ' }] = data[i + 1].trim { it <= ' ' }
                i += 2
            }
        }

        var uri = this.request.uri()
        val index = uri.indexOf('?')
        path = if (index <= 0) {
            uri
        }else {
            val p = uri.substring(0, index)

            uri = uri.substring(index + 1)
            val params = uri.split("[&=]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var i = 0
            while (i < params.size - 1) {
                mParams[params[i]] = params[i + 1]
                i += 2
            }
            p
        }
    }
    fun getStringParam(s: String): String? {
        return mParams[s]
    }

    fun getHeader(origin: AsciiString?): String? {
        return request.headers().get(HttpHeaderNames.ORIGIN)
    }

    fun getCookie(key: String): String? {
        return mCookies[key]
    }

    fun getIntParam(key: String): Int? {
        return mParams[key]?.toIntOrNull()
    }

    fun uri(): String {
        return request.uri()
    }

    fun getStringFromParamOrCookie(key: String): String? {
        return mParams[key] ?: mCookies[key]
    }
}