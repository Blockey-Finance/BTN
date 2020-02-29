package org.btn.masters

import org.btn.common.*
import org.btn.core.Block
import org.btn.core.BlockChain
import org.btn.core.NodeInstance
import org.btn.core.Vote

abstract class MasterNode: NodeInstance(){
    private val candidateBlocks = ArrayList<Block>()

    abstract val blockChain: BlockChain
    private lateinit var curBlock: Block
    fun voteForBlock(){
        val block = candidateBlocks.maxBy { it.priority }
            ?:return

        val signedData = sign(privateKey, block.hashCode)
        block.clerk.vote(signedData,publicKey)
    }

    @ExperimentalStdlibApi
    fun compete(block: Block){
        val hash = block.hashCode
        var round = 0
        while(true){
            val bytes = combineByteArray(hash, round)
            val signedData = sign(privateKey, bytes)
            val signedHash = hash(signedData)
            val zeros = leadingZeroCount(signedHash)

            if(btnNetwork.clerkIsReady)
                break

            if(zeros < btnNetwork.clerkZeros){
                round++
                continue
            }

            btnNetwork.broadcast(hash,signedData,publicKey)
            break
        }
    }


    @ExperimentalStdlibApi
    fun onFinalBlockReceived(block: Block){
        if(blockChain.save(block))
            compete(block)
    }

    fun onBlockVoted(vote: Vote){
        val votes = curBlock.votes
        votes.add(vote)

        if(votes.count() >= btnNetwork.clerkRequiredVotes){
            btnNetwork.broadcastFinalBlock(curBlock)
            return
        }
    }

    fun onSealedBlockReceived(sealedBlock: Block){
        btnNetwork.broadcastSealedBlock(sealedBlock)
    }
}