package org.btn.network


import cn.ms668.common.server.BtnHttpRequest
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpMethod

open class User{}
open class Role{
    companion object{
        fun canExecute(user:User?,api:HttpApi):Boolean{return true}
    }
}
abstract class HttpApi{
    var method: HttpMethod? = null
        private set

    abstract fun onRequest(ctx:ChannelHandlerContext, request: BtnHttpRequest, user: User?)
    fun setMethod(method: String?) {
        if (method == null || method.isEmpty()
            || "GET" != method && "POST" != method) {
            this.method = null
            return
        }
        this.method = HttpMethod.valueOf(method)
    }


    fun canBeCalledByUser(user: User?): Boolean {
        return Role.canExecute(user,this)
    }
}
