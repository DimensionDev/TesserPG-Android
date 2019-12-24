package com.sujitech.tessercubecore.wallet

import com.sujitech.tessercubecore.common.extension.toHexString
import com.sujitech.tessercubecore.common.extension.toJson
import com.sujitech.tessercubecore.common.wallet.RedPacketPayloadHelper
import com.sujitech.tessercubecore.common.wallet.RedPacketRawPayload
import com.sujitech.tessercubecore.common.wallet.toRawPayload
import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.data.RedPacketData
import com.sujitech.tessercubecore.data.RedPacketStatus
import org.web3j.protocol.core.methods.response.TransactionReceipt

class PendingRedPacketHandler : RedPacketHandler() {
    override fun getNonce(redPacketData: RedPacketData): Int {
        return redPacketData.createNonce!!
    }

    override fun getFunctionCall(redPacketData: RedPacketData): String {
        return redPacketData.creationFunctionCall!!
    }

    override fun getTransactionHash(redPacketData: RedPacketData): String {
        return redPacketData.creationTransactionHash!!
    }

    override suspend fun onSuccess(contract: HappyRedPacket, redPacketData: RedPacketData, transactionReceipt: TransactionReceipt) {
        val event = contract.getCreationSuccessEvents(transactionReceipt).first()
        redPacketData.redPacketId = event.id.toHexString()
        redPacketData.status = RedPacketStatus.normal
        redPacketData.rawPayload = redPacketData.toRawPayload().toJson(RedPacketRawPayload.serializer())
        redPacketData.encPayload = RedPacketPayloadHelper.generate(redPacketData)
        saveResult(redPacketData)
    }

    override suspend fun onRevert(redPacketData: RedPacketData, revertReason: String?) {
        redPacketData.status = RedPacketStatus.fail
        redPacketData.failReason = revertReason
        saveResult(redPacketData)
    }
}