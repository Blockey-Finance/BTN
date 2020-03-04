package org.btn.masters

import io.netty.channel.Channel
import io.netty.util.AttributeKey

private val BCA_XID = AttributeKey.newInstance<String>("BCA_XID")
var  Channel?.xid: String?
    get(){
        return this?.attr(BCA_XID)?.get()
    }
    set(value){
        this?.attr(BCA_XID)?.set(value)
    }

private val BCA_UUID = AttributeKey.newInstance<String>("BCA_UUID")
var  Channel?.uuid: String?
    get(){
        return this?.attr(BCA_UUID)?.get()
    }
    set(value){
        this?.attr(BCA_UUID)?.set(value)
    }

private val BCA_TOKEN = AttributeKey.newInstance<String>("BCA_TOKEN")
var  Channel?.token: String?
    get(){
        return this?.attr(BCA_TOKEN)?.get()
    }
    set(value){
        this?.attr(BCA_TOKEN)?.set(value)
    }

private val BCA_FRONT_CHANNEL = AttributeKey.newInstance<Channel>("BCA_FRONT_CHANNEL")
var  Channel?.frontChannel: Channel?
    get(){
        return this?.attr(BCA_FRONT_CHANNEL)?.get()
    }
    set(value){
        this?.attr(BCA_FRONT_CHANNEL)?.set(value)
    }

private val BCA_LAST_IN_TIME = AttributeKey.newInstance<Long>("BCA_LAST_IN_TIME")
var  Channel.lastReceiveTime: Long
    get(){
        val t = this.attr(BCA_LAST_IN_TIME)?.get()
        return t ?: Long.MIN_VALUE
    }
    set(value){
        this.attr(BCA_LAST_IN_TIME)?.set(value)
    }

private val BCA_LAST_OUT_TIME = AttributeKey.newInstance<Long>("BCA_LAST_OUT_TIME")
var  Channel.lastSendTime: Long
    get(){
        val t = this.attr(BCA_LAST_OUT_TIME)?.get()
        return t ?: Long.MIN_VALUE
    }
    set(value){
        this.attr(BCA_LAST_OUT_TIME)?.set(value)
    }