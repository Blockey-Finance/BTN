package cn.ms668.common.server

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object GsonUtils {
    fun gson(): Gson
    {
        return GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    }

    inline fun <reified T : Any> fromJson(json: String): T {
        return Gson().fromJson(json, T::class.java)
    }
}
