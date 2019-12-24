package com.sujitech.tessercubecore.wallet

import com.sujitech.tessercubecore.appContext
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.wallet.getDefaultGasProvider
import com.sujitech.tessercubecore.common.wallet.redPacketContractAddress
import com.sujitech.tessercubecore.common.wallet.web3j
import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.RedPacketData
import com.sujitech.tessercubecore.data.WalletData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.web3j.crypto.WalletUtils
import java.util.concurrent.LinkedBlockingDeque

object RedPacketUpdater {
    private val taskCreator = {
        GlobalScope.async {
            val wallet = DbContext.data.select(WalletData::class).get().firstOrNull() ?: return@async
            val mnemonic = UserPasswordStorage.get(appContext, wallet.mnemonicId)
            val password = UserPasswordStorage.get(appContext, wallet.passwordId)
            val credential = WalletUtils.loadBip39Credentials(password, mnemonic)
            val contract = HappyRedPacket.load(
                    redPacketContractAddress,
                    web3j,
                    credential,
                    getDefaultGasProvider()
            )
            while (!queue.isEmpty()) {
                val redPacketData = queue.take()
                val handler = redPacketData.status.createHandler() ?: continue
                handler.processingRedPacket(redPacketData, contract)
            }
        }
    }
    private var task = taskCreator.invoke()

    private val queue = LinkedBlockingDeque<RedPacketData>()

    fun put(redPacketData: RedPacketData) {
        queue.put(redPacketData)
        if (task.isCompleted) {
            task = taskCreator.invoke()
        }
        if (!task.isActive) {
            task.start()
        }
    }

    fun put(redPacketData: List<RedPacketData>) {
        redPacketData.forEach {
            put(it)
        }
    }
}