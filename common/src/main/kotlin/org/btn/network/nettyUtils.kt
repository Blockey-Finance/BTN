package org.btn.network

import io.netty.channel.Channel
import io.netty.handler.stream.ChunkedStream
import io.netty.util.concurrent.Future
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.btn.common.Log

private val mslog = Log("NettyUtil")
suspend fun writeAndFlushAsync(channel: Channel, msg:Any):Boolean{
    if(msg is ChunkedStream){
        throw Exception("Should not write stream with this function.")
    }
    val waiter = CompletableDeferred<Future<in Void>>()
    val cf = channel.writeAndFlush(msg)
    cf.addListener {
        waiter.complete(it)
        mslog.info("Write and flush finished=$it")
    }
    val ff = withTimeoutOrNull(5000){waiter.await()}
    if(ff== null){
        mslog.err("WriteAndFlushAsync timeout", Exception("Stacktrace is"))
    }else if(!ff.isSuccess){
        mslog.err("WriteAndFlushAsync exception", Exception("Root cause is",ff.cause()))
    }
    return ff?.isSuccess == true

}