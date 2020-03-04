package org.btn.masters

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpRequestEncoder
import io.netty.handler.codec.http.HttpResponseDecoder
import org.btn.common.curTime
import org.btn.network.BtnCmd
import org.btn.network.CmdCodec


class MasterChannelInitializer :  ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch1: SocketChannel?) {
        val pipe = ch1?.pipeline()
            ?: return

        with(pipe) {
            addLast("msCmdCodec", CmdCodec())
            addLast("requestEncoder", HttpRequestEncoder())
            addLast("responseDecoder", HttpResponseDecoder())
            addLast("writeTimeRecorder",object: ChannelOutboundHandlerAdapter(){
                override fun write(ctx: ChannelHandlerContext?, msg: Any?, promise: ChannelPromise?) {
                    if(msg !is BtnCmd)
                        ctx?.channel()?.lastSendTime = curTime()

                    super.write(ctx, msg, promise)
                }
            })
        }
    }
}
