package org.btn.common

import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil

import java.math.BigInteger
import java.security.*
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec


const val RSA_ALG = "RSA/ECB/PKCS1Padding"
const val AES_ALG = "AES/ECB/PKCS5padding" //notice must be set for different vm
private const val RSA_KF = "RSA"
private const val KEYSIZE = 2048


fun aesEnc(s:String):Pair<ByteArray,ByteArray>{
    val generator = KeyGenerator.getInstance("AES")
    generator.init(128) // The AES key size in number of bits
    val aesKey = generator.generateKey()
    val aesCipher = Cipher.getInstance(AES_ALG)
    aesCipher.init(Cipher.ENCRYPT_MODE, aesKey)
    //notice encrypt with aes eky and convert to base 64
    val aesBytes = aesCipher.doFinal(s.toByteArray())
    return Pair(aesKey.encoded,aesBytes)
}
fun aesDec(key:ByteArray,data:ByteArray):String?{
    val aesKey = SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance(AES_ALG)
    cipher.init(Cipher.DECRYPT_MODE, aesKey)

    //notice decode with aes
    val token = String(cipher.doFinal(data))
    return token
}
fun base64Dec(s:String):ByteArray{
    val buf = Unpooled.wrappedBuffer(s.toByteArray(CharsetUtil.US_ASCII))
    val buf1 = io.netty.handler.codec.base64.Base64.decode(buf)
    val bytes = ByteArray(buf1.readableBytes())
    buf1.readBytes(bytes)
    buf1.release()
    buf.release()
    return bytes
}

fun base64Enc(bytes:ByteArray?):String{
    val buf = Unpooled.wrappedBuffer(bytes)
    val buf1 = io.netty.handler.codec.base64.Base64.encode(buf, 0,buf.readableBytes(),false)
    val s = buf1.toString(CharsetUtil.US_ASCII)
    buf1.release()
    buf.release()
    return s
}

private fun getAsymKey(bytes:ByteArray?, isPub:Boolean): Key {
    lateinit var keySpec: EncodedKeySpec
    val keyFactory = KeyFactory.getInstance(RSA_KF)
    return if(isPub){
        keySpec = X509EncodedKeySpec(bytes)
        val k = keyFactory.generatePublic(keySpec)
        k
    }else{
        keySpec = PKCS8EncodedKeySpec(bytes)
        keyFactory.generatePrivate(keySpec)
    }
}



fun sign(privBytes:ByteArray?,data:ByteArray):ByteArray{
    return asymEncrypt(privBytes,data)
}

fun asymEncrypt(privKey:ByteArray?, data:ByteArray, keyIsPub:Boolean = false):ByteArray{
    val key = getAsymKey(privKey,keyIsPub)
    val cipher = Cipher.getInstance(RSA_ALG)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    return cipher.doFinal(data)
}

fun asymDecrypt(pubBytes:ByteArray?, data:ByteArray, keyIsPub:Boolean = true):ByteArray{
    val key = getAsymKey(pubBytes,keyIsPub)
    val cipher = Cipher.getInstance(RSA_ALG)
    cipher.init(Cipher.DECRYPT_MODE, key)
    val dec = cipher.doFinal(data)
    return dec
}

fun rsaKeyPair(): KeyPair? {
    val mslog = BtnLog("BinaryKP")
    mslog.info("start")
    val secureRandom = SecureRandom()
    val keyPairGenerator = KeyPairGenerator.getInstance(RSA_KF)
    keyPairGenerator.initialize(KEYSIZE, secureRandom)
    val keyPair = keyPairGenerator.generateKeyPair()
    return keyPair
}