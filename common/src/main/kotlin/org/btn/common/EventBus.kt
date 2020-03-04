package org.btn.common


import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

open class BtnEvent(public val name:String = "BtnEvent")
typealias Handler<T> = (e:T)->Unit

object EventBus{
    val mslog = Log("EventBus")
    val handlerMap = ConcurrentHashMap<KClass<*>,ArrayList<Handler<*>>>()
    inline fun <reified T: BtnEvent>register(noinline handler: Handler<T>){
        var handlers = handlerMap[T::class] //get(T::class)
        if(handlers == null){
            handlers = ArrayList<Handler<*>>()
            handlerMap[T::class] = handlers
        }
        handlers.add(0,handler)
    }
    inline fun <reified T: BtnEvent>unRegister(noinline handler: Handler<T>){
        val handlers = handlerMap[T::class]
            ?: return
        handlers.remove(handler)
        mslog.info("handler size for ${T::class}"+handlers.size)
    }
    inline fun <reified T: BtnEvent>post(event:T){
        val handlers = handlerMap[T::class] ?: return
        for(handler in handlers){
            @Suppress("UNCHECKED_CAST")
            val h = handler as? Handler<T>
            if(h!= null)
                h(event)
        }
    }

}

fun main(){
//    val handler1: Handler<DataEvent> = {println("handler1${it.name}")}
//    val handler2: Handler<DataEvent> = {println("handler2${it.name}")}
//    val testHandler: Handler<TestEvent> = {println("test handler2${it.name}")}
//
//    EventBus.register(handler1)
//    EventBus.register(handler2)
//    EventBus.register(testHandler)
//    EventBus.post(DataEvent())
//    EventBus.post(TestEvent())
//    EventBus.unRegister(handler1)
//    EventBus.unRegister(handler2)
//    EventBus.unRegister(testHandler)
}
