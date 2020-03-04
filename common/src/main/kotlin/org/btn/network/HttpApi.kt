package cn.ms668.common.server

import cn.ms668.common.Role
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpMethod
import qxj.twt.entities.User

abstract class HttpApi{
    var method: HttpMethod? = null
        private set

    abstract fun onRequest(ctx:ChannelHandlerContext, request:MsHttpRequest, user: User?)
    fun setMethod(method: String?) {
        if (method == null || method.isEmpty()
            || "GET" != method && "POST" != method) {
            this.method = null
            return
        }
        this.method = HttpMethod.valueOf(method)
    }

    //是否有权限执行该api
//    open fun hasRight(user: User?): Boolean {
//        return when {
//            right == 0 -> true
//            user == null -> false
//            user.hasPermission(right) -> true
//            else -> false
//        }
//    }

    fun canBeCalledByUser(user: User?): Boolean {
        return Role.canExecute(user,this)
    }
}
