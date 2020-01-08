package com.sujitech.tessercubecore.wallet

import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.data.RedPacketData
import com.sujitech.tessercubecore.data.RedPacketStatus
import org.web3j.protocol.core.methods.response.TransactionReceipt

class RefundPendingRedPacketHandler: RedPacketHandler() {
    override fun getNonce(redPacketData: RedPacketData): Int {
        return redPacketData.refundNonce!!
    }

    override fun getFunctionCall(redPacketData: RedPacketData): String {
        return redPacketData.refundFunctionCall!!
    }

    override fun getTransactionHash(redPacketData: RedPacketData): String {
        return redPacketData.refundTransactionHash!!
    }

    override suspend fun onSuccess(contract: HappyRedPacket, redPacketData: RedPacketData, transactionReceipt: TransactionReceipt) {
        val event = contract.getRefundSuccessEvents(transactionReceipt).first()
        redPacketData.refundAmount = event.remaining_balance.toBigDecimal()
        redPacketData.status = RedPacketStatus.refunded
        saveResult(redPacketData)
    }

    override suspend fun onRevert(redPacketData: RedPacketData, revertReason: String?) {
        if (revertReason == null) {
            return
        }
        redPacketData.status = RedPacketStatus.expired
    }
}