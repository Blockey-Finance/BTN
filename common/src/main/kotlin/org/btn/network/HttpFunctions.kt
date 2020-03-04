package org.btn.network

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelProgressiveFuture
import io.netty.channel.ChannelProgressiveFutureListener
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL
import io.netty.handler.codec.http.HttpHeaderNames.TRANSFER_ENCODING
import io.netty.handler.codec.http.HttpHeaderValues.CHUNKED
import io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT
import io.netty.handler.stream.ChunkedStream
import io.netty.util.AttributeKey
import io.netty.util.ReferenceCountUtil
import org.btn.common.Log

import org.reflections.Reflections
import java.io.*
import java.util.*


private val mslog = Log("HttpFunctions", false)
val LAST_REQUEST_ORIGIN: AttributeKey<String?> = AttributeKey.newInstance<String?>("LAST_REQUEST_ORIGIN")

fun replyHttpOption(ctx: ChannelHandlerContext, origin: String?) {
    val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK) //,buf
    HttpUtil.setContentLength(response, 0)
    val headers = response.headers()

    with(headers) {
        set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8")
        set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE,OPTIONS")
        set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-type, Content-Length, Origin, x-ijt")
    }

    sendResponse(ctx, response)
}

fun sendCode(
    ctx: ChannelHandlerContext
    , status: HttpResponseStatus
    , phrase: String? = ""
    , response: HttpResponse = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
) {
    val status1 = if (phrase.isNullOrEmpty()) status else HttpResponseStatus(status.code(), phrase)
    HttpUtil.setContentLength(response, 0)
    response.status = status1
    sendResponse(ctx, response)
}

fun sendResponse(ctx: ChannelHandlerContext, response: HttpResponse): ChannelFuture {
    allowOrigin(ctx, response)
    val prom1 = ctx.writeAndFlush(response)
    if (response is FullHttpResponse)
        return prom1

    val prom2 = ctx.newPromise()
    prom1.addListener {
        if (it.isSuccess)
            ctx.writeAndFlush(EMPTY_LAST_CONTENT, prom2)
        else
            prom2.setFailure(it.cause())
    }
    return prom2
}

fun sendApiCode(ctx: ChannelHandlerContext, code: ApiCode) {
    sendJsonCode(ctx, code.code, code.desc)
}

fun sendJsonCode(ctx: ChannelHandlerContext, code: Int, desc: String = "") {
    sendJson(ctx, """{"code":$code,"desc":"$desc"}""")
}

fun sendJsonObject(
    ctx: ChannelHandlerContext,
    jsonObj: String,
    desc: String = "",
    resp: HttpResponse = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
) {
    sendJson(ctx, """{"code":0,"obj":$jsonObj,"desc":"$desc"}""", resp)
}

fun sendJson(
    ctx: ChannelHandlerContext,
    json: String,
    response: HttpResponse = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
) {


    if (response is FullHttpResponse) {
        mslog.err("Should not use full http response")
        throw Exception("Full response can not write json")
    }

    allowOrigin(ctx, response)
    val buf = Unpooled.wrappedBuffer(json.toByteArray())
    val headers = response.headers()
    headers[HttpHeaderNames.CONTENT_LENGTH] = buf.readableBytes()
    headers[HttpHeaderNames.CONTENT_TYPE] = "application/json"
    headers[CACHE_CONTROL] = "no-store"
    headers[HttpHeaderNames.PRAGMA] = "no-cache"
    ctx.writeAndFlush(response)
    ctx.writeAndFlush(buf) //notice netty will release
    ctx.writeAndFlush(EMPTY_LAST_CONTENT)
}

private fun allowOrigin(ctx: ChannelHandlerContext, response: HttpResponse) {
    val origin = ctx.channel().attr(LAST_REQUEST_ORIGIN)
    if (origin != null)
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin)

    response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, true)
}

fun sendFile(
    ctx: ChannelHandlerContext,
    request: HttpRequest,
    response: DefaultHttpResponse,
    filePath: String
) {
    try {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, getContentType(filePath))
        val file = File(filePath)
        val etag = file.lastModified()
        val clientTag = request.headers()[HttpHeaderNames.IF_NONE_MATCH]?.toLongOrNull()
//        if(clientTag != null)
//            mslog.info("etag=$etag clientTag=$clientTag is equal=${etag == clientTag}")
        if (clientTag == etag) {
            sendCode(ctx, HttpResponseStatus.NOT_MODIFIED)
            return
        }

        response.headers().set(CACHE_CONTROL, "max-age=0")
        response.headers().set(HttpHeaderNames.ETAG, etag)
        sendStream(ctx, request, response, FileInputStream(file), file.length())
    } catch (e: Exception) {
        sendCode(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, e.message)
    }
}

private val mContentTypes = mapOf(
    "js" to "application/javascript"
    , "json" to "application/json"
    , "png" to "image/png"
    , "jpg" to "image/jpeg"
    , "gif" to "image/gif"
    , "html" to "text/html"
    , "css" to "text/css"
    , "mp4" to "video/mp4"
    , "mov" to "video/quicktime"
    , "avi" to "video/x-msvideo"
    , "zip" to "application/zip"
)

fun getContentType(file: String): String {
    val index = file.lastIndexOf(".")
    if (index != -1) {
        var e: String? = file.substring(index + 1)
        e = mContentTypes[e!!]
        if (e != null) {
            return e
        }
    }
    return "text/plain"
}

fun sendStream(
    ctx: ChannelHandlerContext,
    request: HttpRequest,
    response: HttpResponse,
    istream: InputStream,
    fileLen: Long
) {
    val sw = Log("SendStreamImpl", true)
    var start: Long = 0
    var end = fileLen - 1

    allowOrigin(ctx, response)
    val range = request.headers()[HttpHeaderNames.RANGE]
    response.status = HttpResponseStatus.OK
    val headers = response.headers()
    if (range != null) {
        sw.err("FFFF parsing range thread id=" + Thread.currentThread().id)

        var parts = range.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size != 2 || "bytes" != parts[0]) {
            sendCode(ctx, HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
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
                fileLen - 1

            response.status = HttpResponseStatus.PARTIAL_CONTENT
            headers.set(
                HttpHeaderNames.CONTENT_RANGE,
                String.format(Locale.ENGLISH, "bytes %d-%d/%d", start, end, fileLen)
            )

            sw.log("FFFF parsing range successful total=$fileLen start=$start end=$end")
        } catch (e: Exception) {
            sendCode(ctx, HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE, e.message)
            return
        }
    }

    try {
        if (start != istream.skip(start)) {
            sw.log("FFFF skip failed to skip requested amount")
            return
        }
        sw.log("FFFF after skip val=$start")
        val len = end - start + 1
        headers.set(HttpHeaderNames.CONTENT_LENGTH, len.toString())
        headers.set(HttpHeaderNames.ACCEPT_RANGES, "bytes")
        headers.set(TRANSFER_ENCODING, CHUNKED)
        ctx.writeAndFlush(response)
        val chunkStream = ChunkedStream(istream)
        val ci = HttpChunkedInput(chunkStream)


        ctx.writeAndFlush(ci, ctx.newProgressivePromise()).addListener(object :
            ChannelProgressiveFutureListener {
            override fun operationComplete(future: ChannelProgressiveFuture?) {
                istream.close()
                ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
            }

            override fun operationProgressed(future: ChannelProgressiveFuture?, progress: Long, total: Long) {
                if (progress >= end) {
                    chunkStream.close()
                } else {
//                    mslog.info("write file progress $progress/$total")
                }
            }
        })
        return
    } catch (e: Exception) {
        sendCode(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, e.message)
        istream.close()
        return
    }
}

fun forward(ctx: ChannelHandlerContext, msg: HttpObject) {
    ReferenceCountUtil.retain<HttpObject>(msg)
    ctx.fireChannelRead(msg)
}


