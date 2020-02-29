package org.btn.supervisor

<<<<<<< HEAD
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import org.btn.common.*
import org.btn.core.Block
import org.btn.core.NodeInstance
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel



abstract class SupervisorNode : NodeInstance() {
    @ExperimentalStdlibApi
    fun sealBlock(block: Block) {
        if (!block.verify())
            return

        val signedData = sign(privateKey, block.hashCode)
        val hash = hash(signedData)
        val precedingZeroCount = leadingZeroCount(hash)
        if (precedingZeroCount <= btnNetwork.sealPrecedingThreshold)
            return

        block.clerk.seal(signedData, publicKey)
    }
}
