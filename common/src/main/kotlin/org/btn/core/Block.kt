package org.btn.core

import MasterNodeStub

abstract class Block{
    abstract val votes: ArrayList<Vote>
    abstract val sealIsValid: Boolean
    abstract val voteIsValid: Boolean
    abstract val priority: Int
    abstract val hashCode:ByteArray
    abstract val clerk: MasterNodeStub
    fun verify():Boolean{
        return true
    }

}