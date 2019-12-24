package com.sujitech.tessercubecore.wallet

import com.sujitech.tessercubecore.common.extension.await
import com.sujitech.tessercubecore.common.wallet.*
import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.RedPacketData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.TransactionReceipt

abstract class RedPacketHandler: IRedPacketHandler {
    override suspend fun processingRedPacket(redPacketData: RedPacketData, contract: HappyRedPacket) {
        val transactionHash = getTransactionHash(redPacketData)
        val transactionReceipt = tryGetTransactionReceipt(web3j, transactionHash) ?: return
        if (transactionReceipt.isStatusOK) {
            onSuccess(contract, redPacketData, transactionReceipt)
        } else {
            val functionCall = getFunctionCall(redPacketData)
            val nonce = getNonce(redPacketData)
            val ethCall = web3j.ethCall(Transaction(
                    redPacketData.senderAddress,
                    nonce.toBigInteger(),
                    defaultGasPrice,
                    defaultGasLimit,
                    redPacketData.contractAddress,
                    redPacketData.sendTotal.toBigInteger(),
                    functionCall), DefaultBlockParameterName.LATEST).sendAsync().await()
            onRevert(redPacketData, ethCall.revertReason)
        }
    }

    protected suspend fun saveResult(redPacketData: RedPacketData) {
        withContext(Dispatchers.Main) {
            DbContext.data.update(redPacketData).blockingGet()
        }
    }

    protected abstract fun getNonce(redPacketData: RedPacketData): Int
    protected abstract fun getFunctionCall(redPacketData: RedPacketData): String
    protected abstract fun getTransactionHash(redPacketData: RedPacketData): String
    protected abstract suspend fun onSuccess(contract: HappyRedPacket, redPacketData: RedPacketData, transactionReceipt: TransactionReceipt)
    protected abstract suspend fun onRevert(redPacketData: RedPacketData, revertReason: String?)

    private suspend fun tryGetTransactionReceipt(web3j: Web3j, transactionHash: String): TransactionReceipt? {
        for (i in (0 until retryCount)) {
            val ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(transactionHash).sendAsync().await()
            if (ethGetTransactionReceipt.transactionReceipt.isPresent) {
                return ethGetTransactionReceipt.transactionReceipt.get()
            }
            delay(blockTime)
        }
        return null
    }

}