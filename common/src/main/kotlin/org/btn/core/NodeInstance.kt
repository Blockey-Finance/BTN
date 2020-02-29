package org.btn.core

abstract class NodeInstance: Node(){
    protected abstract val btnNetwork: BtnNetwork
    protected abstract val privateKey:ByteArray
}