package org.btn.masters

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel

const val PRIMARY_PORT = 10001

fun main() {
    val mBoss = NioEventLoopGroup(1) //for listen
    val mWorker = NioEventLoopGroup(4) //for processing i/o of each socket/channel

    val bs = ServerBootstrap()
    try {
        bs.group(mBoss, mWorker)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 128)
            .option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(SupervisorChannelInitializer())

        val f = bs.bind(PRIMARY_PORT).sync()
        if (f.isSuccess) {
            println("Primary node is started")
            f.channel().closeFuture().addListener {
                mWorker.shutdownGracefully()
                mBoss.shutdownGracefully()
            }
        } else {
            mWorker.shutdownGracefully()
            mBoss.shutdownGracefully()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}