package cn.ms668.common.server

import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import io.netty.util.AsciiString
import io.netty.util.CharsetUtil
import java.util.concurrent.ConcurrentHashMap

//QueryStringDecoder?
//CookieDecoder

class BtnHttpRequest(val nettyHttpRequest: HttpRequest):HttpRequest by nettyHttpRequest{
    private val mParams = ConcurrentHashMap<String,String>()
    private val mCookies =  ConcurrentHashMap<String,String>()
//    lateinit var cookies: Set<Cookie>
    private val path:String

    init {
        val header = nettyHttpRequest.headers().get(HttpHeaderNames.COOKIE)
        if (header != null) {
//            cookies = ClientCookieDecoder(false).decode(request.headers()[HttpHeaderNames.COOKIE])
            val data = header.split("[;=]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var i = 0
            while (i < data.size - 1) {
                mCookies[data[i].trim { it <= ' ' }.toLowerCase()] = data[i + 1].trim { it <= ' ' }
                i += 2
            }
        }

        var uri = this.nettyHttpRequest.uri()
        val index = uri.indexOf('?')
        path = if (index <= 0) {
            uri
        }else {
            val p = uri.substring(0, index)

            uri = uri.substring(index + 1)
            val params = uri.split("[&=]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var i = 0
            while (i < params.size - 1) {
                mParams[params[i].toLowerCase()] = params[i + 1]
                i += 2
            }
            p
        }
    }
    fun getStringParam(s: String, default:String? = null): String? {
        return mParams[s.toLowerCase()] ?: default
    }

    fun getHeader(headerName: AsciiString?): String? {
        return nettyHttpRequest.headers().get(headerName)
    }

    fun getCookie(key: String): String? {
        return mCookies[key.toLowerCase()]
    }

    fun getIntParam(key: String): Int? {
        return getStringParam(key)?.toIntOrNull()
    }

    fun getStringFromParamOrCookie(key: String): String? {
        return mParams[key] ?: mCookies[key]
    }

    fun toFullHttpRequestOrNull(): FullHttpRequest? {
        return if(nettyHttpRequest is FullHttpRequest)
            nettyHttpRequest
        else
            null
    }

    fun getBooleanParam(key: String): Boolean {
        return getStringParam(key)?.toBoolean() == true
    }

    fun getLongParam(s: String): Long? {
        return getStringParam(s)?.toLongOrNull()
    }

    fun getPostDataStr(): String? {
        val full = toFullHttpRequestOrNull() ?: return null
        val content = full.content() ?: return null
        if (content.readableBytes() == 0) {
            return null
        }
        val data = ByteArray(content.readableBytes())
        content.getBytes(0, data)
        return try {
            val charset = full.headers()[HttpHeaderNames.ACCEPT_CHARSET] ?: CharsetUtil.UTF_8.displayName()
            return java.lang.String(data, charset) as String
        } catch (e: Exception) {
            null
        }
    }
}
