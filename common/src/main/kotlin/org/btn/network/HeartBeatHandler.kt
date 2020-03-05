package org.btn.network


import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import org.btn.common.Log

const val CMD_HEART = "heartbeat"
class HeartBeatHandler(private val name: String) : ChannelInboundHandlerAdapter() {
    /**
     *  test test
     */

    private var writeCount = 0
    private val mslog = Log("HeartBeat",false)
    private var ctx: ChannelHandlerContext? = null
    private var lastReadTime = System.currentTimeMillis()

    init {
    }

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
//        log1.warn("Heartbeat, fire channel read")
        super.channelRead(ctx, msg)
    }

    override fun channelActive(ctx: ChannelHandlerContext?) {
        super.channelActive(ctx)
        this.ctx = ctx
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        super.channelInactive(ctx)
        this.ctx = null
    }
    override fun userEventTriggered(ctx: ChannelHandlerContext?, evt: Any?) {
        if(evt !is IdleStateEvent)
            return

        val state = evt.state()
        //mslog.info("UserEvent triggered state=$state")
        if (state == IdleState.WRITER_IDLE) {
            val last = ctx?.pipeline()?.last()
            var s = ""+last

            writeCount ++
            if(writeCount < 5 || writeCount % 20 == 0)
                mslog.info("Heartbeat:$name [$writeCount] Send Heart Beat for handler [$s] because write idle")

            sendMsCmdStr(ctx, CMD_HEART)
        }else if(state == IdleState.READER_IDLE) {
            mslog.err("Heartbeat:$name Channel read timeout, should receive heart beat but did not, read time out, close the channel")
            ctx?.close()
        }
    }
}