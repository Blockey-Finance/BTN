package org.btn.core

import org.btn.core.Block

class BtnNetwork{
    fun broadcast(hash: ByteArray, signedData: ByteArray, publicKey: ByteArray) {

    }

    fun broadcastFinalBlock(curBlock: Block) {

    }

    fun broadcastSealedBlock(sealedBlock: Block) {

    }

    val clerkRequiredVotes: Int
        get() = 30
    val clerkIsReady: Boolean
        get() = false
    val clerkZeros: Int
        get() = 10
    val sealPrecedingThreshold: Int
        get() {return 10}
}