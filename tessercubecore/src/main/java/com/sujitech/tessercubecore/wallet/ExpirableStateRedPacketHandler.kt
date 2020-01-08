package com.sujitech.tessercubecore.wallet

import com.sujitech.tessercubecore.common.extension.await
import com.sujitech.tessercubecore.common.extension.hexStringToByteArray
import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.RedPacketData
import com.sujitech.tessercubecore.data.RedPacketStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExpirableStateRedPacketHandler : IRedPacketHandler {
    override suspend fun processingRedPacket(redPacketData: RedPacketData, contract: HappyRedPacket) {
        if (redPacketData.blockCreationTime!! + redPacketData.duration > System.currentTimeMillis() / 1000) {
            return
        }
        val result = contract.check_availability(redPacketData.redPacketId!!.hexStringToByteArray()).sendAsync().await()
        if (result.component4()) {
            redPacketData.status = RedPacketStatus.expired
            withContext(Dispatchers.Main) {
                DbContext.data.update(redPacketData).blockingGet()
            }
        }
    }
}