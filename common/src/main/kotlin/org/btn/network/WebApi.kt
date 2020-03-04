package org.btn.network

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class WebApi(
    val name: String = ""
    , val method: String = ""
    , val right: Int = 0
)
