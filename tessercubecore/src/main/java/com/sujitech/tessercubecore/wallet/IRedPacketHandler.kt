package com.sujitech.tessercubecore.wallet

import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.data.RedPacketData
import com.sujitech.tessercubecore.data.RedPacketStatus

interface IRedPacketHandler {
    suspend fun processingRedPacket(redPacketData: RedPacketData, contract: HappyRedPacket)
}

fun RedPacketStatus.createHandler() : IRedPacketHandler? {
    return when (this) {
        RedPacketStatus.pending -> PendingRedPacketHandler()
        RedPacketStatus.claimPending -> ClaimPendingRedPacketHandler()
        RedPacketStatus.refundPending -> RefundPendingRedPacketHandler()
        else -> null
    }
}

