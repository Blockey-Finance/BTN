package org.btn.masters

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.btn.common.Log
import org.btn.common.PRIMARY_PORT
import org.btn.network.HttpApi
import org.btn.server.WebApi
import org.reflections.Reflections
import java.util.LinkedHashMap

val httpApis = LinkedHashMap<String, HttpApi>()
private val log = Log("HttpApi")
fun prepareHttpApis(apiPackageName: String, apiBasePath: String) {
    log.info("prepare api begin")
//    val pkgname = HttpApi::class.java.`package`.name
    log.info("base Api Pkgname=$apiPackageName")

    val refs = Reflections(apiPackageName)
    val classes = refs.getSubTypesOf(HttpApi::class.java)
    var callback: HttpApi
    for (cls in classes) {
        //notice 必须含MsWebApi注解
        val api = cls.getAnnotation(WebApi::class.java) ?: continue
        var name = api.name
        if (name.isEmpty()) {
            name = cls.simpleName.toLowerCase()
        }
        val thisApiPackage = cls.`package`.name
        val subPackage =
                if (thisApiPackage == apiPackageName) "" else thisApiPackage.replace("$apiPackageName.", "") + "/"
        name = "/$apiBasePath/$subPackage$name"
//        mslog.err("final name=$name")
        try {
            callback = cls.getConstructor().newInstance() as HttpApi
        } catch (e: Exception) {
            continue
        }

        callback.setMethod(api.method.trim { it <= ' ' }.toUpperCase())
        httpApis[name] = callback
    }
    log.info("prepare api end")
}

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
            .childHandler(MasterChannelInitializer())

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