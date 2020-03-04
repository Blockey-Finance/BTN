package cn.ms668.common.server

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class MsWebApi(
    val name: String = "" //默认就是类名全部小写的结果
    , val method: String = "" //默认是空字符串，同时支持GET和POST（目前也仅支持GET和POST）
    , val right: Int = 0 //表示需要的权限，默认不需要权限
)
