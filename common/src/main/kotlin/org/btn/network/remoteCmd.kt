package org.btn.network

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.CombinedChannelDuplexHandler
import io.netty.handler.codec.MessageToByteEncoder
import org.btn.common.Log
import java.nio.charset.Charset

private val enclog = Log("BtnCmd Encoder", true)
private val declog = Log("BtnCmdDecoder", true)
const val BTN_CMD_FLAG = 0x7F1A2B3C
val CHARSET: Charset = Charset.forName("UTF-8")
val BTN_PING_CMD = BtnCmd("PING{}")
const val CMD_PONG_STR = "PONG"
val PONG = BtnCmd("$CMD_PONG_STR{}")

open class BtnCmd(open val body:String ){
    val cmd:String
    val param:JsonObject?
    val paramString:String?
    init {
        val index = body.indexOf("{")
        if(index == -1){
            cmd = body
            paramString = ""
            param = null
        }else{
            cmd = body.substring(0,index)
            paramString =  body.substring(index)
            param = try{
                JsonParser().parse(paramString).asJsonObject
            }catch (e:Exception){
                e.printStackTrace()
                null
            }
        }
    }
    fun getStringParam(name:String):String?{
        return param?.get(name)?.asString
    }

    fun getLongParam(name:String):Long?{
        return param?.get(name)?.asLong
    }

    fun getIntParam(name:String):Int?{
        return param?.get(name)?.asInt
    }

    fun getJsonParam(name:String):JsonObject?{
        return param?.get(name)?.asJsonObject
    }
    fun getBooleanParam(name:String):Boolean?{
        return param?.get(name)?.asBoolean
    }

    override fun toString(): String {
        return "BtnCMD:[$cmd]$body"
    }
    fun getJsonArray(s: String): JsonArray? {
        return param?.getAsJsonArray(s)
    }

    fun getReturnCode(): Int? {
        return getIntParam("r")
    }
}

open class CmdEncoder: MessageToByteEncoder<BtnCmd>() {
    init {
        enclog.info("BtnCmd Encoder Created")
    }
    override fun acceptOutboundMessage(msg: Any?): Boolean {
        return msg is BtnCmd
    }
    override fun encode(ctx: ChannelHandlerContext?, msg: BtnCmd?, out: ByteBuf?) {
        enclog.info("try to encode message$msg")
        if(out == null)
            return

        if(msg?.body == null){
            enclog.info("read a message ,but body is null")
            return
        }

        enclog.info("Encoding btncmd ${msg.body}")
        out.writeInt(BTN_CMD_FLAG)
        val bytes = msg.body.toByteArray(Charsets.UTF_8)
        out.writeInt(bytes.size)
        out.writeBytes(bytes)
        enclog.info("Encode end")
    }
}

open class CmdDecoder:ChannelInboundHandlerAdapter(){
    private enum class State {
        INIT,
        READ_COMMAND
    }

    private var state = State.INIT
    private var buffer:ByteBuf? = null 
    private var byteRead = 0
    private var messageLen = 0
    private var readCount = 0
    private var cmdId = 0

    init {
        this.resetState()
    }
    private fun resetState(){
        this.buffer?.release()
        this.buffer = null
        this.byteRead = 0
        this.state = State.INIT
        this.messageLen = 0
        this.readCount = 0
    }
    
    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        if(ctx == null){
            declog.info("read a message ,but context is null")
            return
        }


        if(msg !is ByteBuf){
            declog.info("read a message ,not byte buffer")
            ctx.fireChannelRead(msg)
            return
        }

        if(msg.refCnt() == 0){
            declog.err("Command reference count = 0",Error("Wrong input message,Command is not retained."))
            return
        }

        while (true) {
            if(state == State.INIT){
                if(msg.readableBytes() < 8){
                    ctx.fireChannelRead(msg)
                    return
                }

                msg.markReaderIndex()
                declog.info("before try to read btn cmd flag")

                val flag = try{
                    msg.readInt()
                }catch (e:java.lang.Exception){
                    declog.err("message=${msg.hashCode()} channelId=${ctx.channel().id()}")
                    declog.err("decode err",e)
                    msg.release()
                    throw e
                }

                declog.info("after read btn cmd flag")
                if(flag != BTN_CMD_FLAG){
                    msg.resetReaderIndex()
                    ctx.fireChannelRead(msg)
                    return
                }

                declog.info("will change btncmd read state")
                messageLen = msg.readInt()

                state = State.READ_COMMAND 
                cmdId++
                buffer = ctx.alloc().buffer(messageLen)
                if(buffer == null) {
                    msg.release()
                    throw Exception("Can not allocate buffer for BtnCmd")
                }

                declog.info("Ready for a new btn cmd=$cmdId")
            }

            var leftLen = messageLen - byteRead
            if(msg.readableBytes() <= leftLen)
                leftLen = msg.readableBytes()

            readCount ++
            buffer?.writeBytes(msg,leftLen) //notice
            byteRead += leftLen
            if(byteRead >= messageLen){
                val btncmd = BtnCmd(buffer?.readCharSequence(messageLen,CHARSET).toString())
                declog.info("read btn cmd finished cmd=${btncmd.body} remained bytes=${msg.readableBytes()}")
                ctx.fireChannelRead(btncmd)
                this.resetState()
            }

            if (msg.readableBytes() == 0) {
                msg.release()  //notice wrong btng, not fired, must be released
                return
            }else{
                //notice continue until no byte.
            }
        }
    }


}

class CmdCodec : CombinedChannelDuplexHandler<CmdDecoder, CmdEncoder>() {
    init {
        init(CmdDecoder(), CmdEncoder())
    }
}