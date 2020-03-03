package org.btn.supervisor

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import org.btn.common.PRIMARY_PORT

fun main(){
    val bs = Bootstrap()
    val mWorker = NioEventLoopGroup(1)
    bs.group(mWorker)
        .channel(NioSocketChannel::class.java)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
        .handler(SupervisorChannelInitializer())

    val fc = bs.connect("0.0.0.0", PRIMARY_PORT)
    fc.await()
}