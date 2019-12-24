package com.sujitech.tessercubecore.wallet

import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.data.RedPacketData
import com.sujitech.tessercubecore.data.RedPacketStatus
import org.web3j.protocol.core.methods.response.TransactionReceipt

class ClaimPendingRedPacketHandler : RedPacketHandler() {
    override fun getNonce(redPacketData: RedPacketData): Int {
        return redPacketData.claimNonce!!
    }

    override fun getFunctionCall(redPacketData: RedPacketData): String {
        return redPacketData.claimFunctionCall!!
    }

    override fun getTransactionHash(redPacketData: RedPacketData): String {
        return redPacketData.claimTransactionHash!!
    }

    override suspend fun onSuccess(contract: HappyRedPacket, redPacketData: RedPacketData, transactionReceipt: TransactionReceipt) {
        val event = contract.getClaimSuccessEvents(transactionReceipt).first()
        redPacketData.claimAmount = event.claimed_value.toBigDecimal()
        redPacketData.status = RedPacketStatus.claimed
        saveResult(redPacketData)
    }

    override suspend fun onRevert(redPacketData: RedPacketData, revertReason: String?) {
        if (revertReason == null) {
            if (redPacketData.createNonce != null) {
                redPacketData.status = RedPacketStatus.normal
            } else {
                redPacketData.status = RedPacketStatus.incoming
            }
        } else {
            when (revertReason.take(3).toInt()) {
                3 -> redPacketData.status = RedPacketStatus.expired
                4 -> redPacketData.status = RedPacketStatus.empty
                5 -> redPacketData.status = RedPacketStatus.claimed //TODO
                6 -> redPacketData.status = RedPacketStatus.normal
                7 -> redPacketData.status = RedPacketStatus.normal
            }
        }
        saveResult(redPacketData)
    }
}