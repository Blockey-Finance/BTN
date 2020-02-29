package org.btn.common

import java.math.BigInteger
import java.security.MessageDigest

private const val SHA_KEY = "SHA-1"

fun combineByteArray(data:ByteArray, int:Int):ByteArray{
    return ByteArray(10)
}

fun hash(data:ByteArray):ByteArray{
    val md = MessageDigest.getInstance(SHA_KEY)
    val hash = md.digest(data)
    return hash
}

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
fun leadingZeroCount(hash: ByteArray): Int {
    var count = 0
    val zero:Byte = 0
    var curByte:UByte = 0xffu
    for ((index, b) in hash.withIndex()){
        if(b == zero)
            count += 8
        else{
            curByte = b.toUByte()
            break
        }
    }
    return count + curByte.countLeadingZeroBits()
}

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
fun test(i:Int):Int{
    val arr = i.toString().toByteArray()
    val hash = hash(arr)
    val count = leadingZeroCount(hash)

    if(count > 20){
        println(i)
        val s = BigInteger(hash).toString(2)
        println("leading zero count="+count)
    }
    return count
}

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
fun test1(){
    val zero = 0x00
    val b1:UByte = 0x10u
    val b3:UByte = 0x03u
    val b7:UByte = 0x07u
    val bn:UByte = 0xffu

    println(b1.countLeadingZeroBits())
    println(b3.countLeadingZeroBits())
    println(b7.countLeadingZeroBits())
    println(bn.countLeadingZeroBits())
}
@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
fun main() {
    var totalNumber = 0
    for (i in 1..10000000) {
        val count = test(i)
        if (count > 20)
            totalNumber++
    }
    print("total number=" + totalNumber)
}

