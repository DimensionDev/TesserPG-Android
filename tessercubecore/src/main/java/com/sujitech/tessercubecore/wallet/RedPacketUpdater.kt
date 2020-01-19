package com.sujitech.tessercubecore.wallet

import android.util.Log
import com.sujitech.tessercubecore.appContext
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.wallet.getDefaultGasProvider
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
    private val TAG = "RedPacketUpdater"
    private val taskCreator = {
        GlobalScope.async {
            val wallet = DbContext.data.select(WalletData::class).get().firstOrNull() ?: return@async
            val mnemonic = UserPasswordStorage.get(appContext, wallet.mnemonicId)
            val password = UserPasswordStorage.get(appContext, wallet.passwordId)
            val credential = WalletUtils.loadBip39Credentials(password, mnemonic)
            Log.i(TAG, "Updating red packet status")
            while (!queue.isEmpty()) {
                val redPacketData = queue.take()
                Log.i(TAG, "Updating red packet: From: ${redPacketData.senderName} Current: ${redPacketData.status.name}")
                val handler = redPacketData.status.createHandler() ?: continue
                val web3j = redPacketData.network.web3j
                try {
                    val contract = HappyRedPacket.load(
                            redPacketData.contractAddress,
                            web3j,
                            credential,
                            getDefaultGasProvider()
                    )
                    handler.processingRedPacket(redPacketData, contract)
                } catch (e: Throwable) {
                    // Ignore any exception
                    e.printStackTrace()
                } finally {
                    web3j.shutdown()
                }
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