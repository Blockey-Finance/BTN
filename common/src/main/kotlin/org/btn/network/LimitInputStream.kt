package org.btn.network

import org.btn.common.Log
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

class LimitInputStream(input: InputStream, private var mLimit: Long,slient:Boolean = true) : FilterInputStream(input) {
    private var sw = Log("LimitInputStream",true)
    @Throws(IOException::class)
    override fun available(): Int {
        var lim = super.available()
        sw.log("Awailable =$lim")
        if(lim > 0)
            return lim

        sw.err("parent said available < 0 mLimit=$mLimit")
        lim = mLimit.toInt()
        if(lim > 0)
            return lim


        return Integer.MAX_VALUE
    }

    @Throws(IOException::class)
    override fun read(): Int {
        try{
            if (mLimit <= 0) {
                return -1
            }
            val result = super.read()
            if (result >= 0)
                --mLimit

            sw.info("FFFF limit stream read result=$result curlimit=$mLimit thread id="+Thread.currentThread().id)
            return result
        }catch (e:Exception){
            sw.err("Read byte exception "+e.message,e)
            mLimit = 0
            return -1
        }

    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if(off>1){
            sw.info("Offset>1")
        }
        if (mLimit <= 0) {
//            sw.log("FFFF batch read limit stream need to be closed")
            return -1
        }
        var len :Long = len.toLong()
        len = Math.min(len, mLimit)
        val result = super.read(b, off, len.toInt())

        if (result > 0) {
            mLimit -= result
        }
        sw.log("FFFF Buffer read limit stream[off=$off len=$len] read bytes=$result curLimit=$mLimit")
        return result
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        val result = super.skip(Math.min(n, mLimit))
        sw.err("FFFF limit stream read skip result$result")
        if (result > 0) {
            mLimit -= result.toInt()
        }
        return result
    }
}
