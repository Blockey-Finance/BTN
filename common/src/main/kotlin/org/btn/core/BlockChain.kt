package org.btn.core

import org.btn.core.Block

class BlockChain{
    fun save(block: Block):Boolean{
        return block.verify() && block.sealIsValid && block.voteIsValid
    }
}