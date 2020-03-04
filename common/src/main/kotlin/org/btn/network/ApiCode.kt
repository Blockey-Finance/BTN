package org.btn.network

import java.lang.reflect.Modifier

class ApiCode(val code: Int, val desc: String) {
    companion object {

        val SUCCESS = ApiCode(0, "success")
        val PARAM_ERROR = ApiCode(1, "parameter error")
        val DB_ERROR = ApiCode(2, "db error")
        val NOT_FOUND = ApiCode(3, "not found")
        val NOT_ALLOWED = ApiCode(4, "not allowed")
        val EMPTY = ApiCode(5, "empty")
        val USER_LOCKED = ApiCode(6, "User Locked")
        val FETCH_ERROR = ApiCode(7, "Origin Error")


        fun fromCode(code: Int): ApiCode {
            val fields = ApiCode::class.java.declaredFields
            for (f in fields) {
                if (!Modifier.isStatic(f.modifiers) || f.type != ApiCode::class.java) {
                    continue
                }
                f.isAccessible = true
                val apiCode = f.get(ApiCode::class.java) as ApiCode
                if (apiCode.code == code) {
                    return apiCode
                }
            }
            return ApiCode(code, "")
        }
    }
}
