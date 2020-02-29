import org.btn.core.Node

abstract class MasterNodeStub: Node(){
    fun seal(signedData: ByteArray, publicKey: ByteArray) {  }
    abstract fun vote(signedData: ByteArray, publicKey: ByteArray)
}