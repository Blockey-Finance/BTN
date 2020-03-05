package org.btn.supervisor

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import org.btn.common.Log
import org.btn.network.CmdCodec

private val clientLog = Log("SupervisorNode")
class SupervisorChannelInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel?) {
        if(ch == null)
            return

        clientLog.info(" channel")
        val pipe = ch.pipeline()
        pipe.addLast("CmdEncoder",CmdCodec())
        var handlers = "Pipe handlers ======= \n"
        pipe.toMap().forEach {
            handlers += "name=${it.key} object=${it.value}\n"
        }
    }
}
