package com.sujitech.tessercubecore.viewmodel.wallet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.sujitech.tessercubecore.common.extension.await
import com.sujitech.tessercubecore.common.extension.hexStringToByteArray
import com.sujitech.tessercubecore.common.wallet.*
import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.exceptions.TransactionException
import org.web3j.utils.Numeric

class IncomingRedPacketViewModel : ViewModel() {
    val wallets = DbContext.data.select(WalletData::class).get().toList()
    suspend fun openRedPacket(redPacketData: RedPacketData, walletMnemonic: String, walletPassword: String): RedPacketData {
        val credentials = WalletUtils.loadBip39Credentials(walletPassword, walletMnemonic)
        val contractGasProvider = getDefaultGasProvider()
        val contract = HappyRedPacket.load(
                redPacketData.contractAddress,
                web3j,
                credentials,
                contractGasProvider)
        val redPacketId = redPacketData.redPacketId!!.hexStringToByteArray()
        val availability = contract.check_availability(redPacketId).send()
        if (availability.component2() == availability.component3()) {
            redPacketData.status = RedPacketStatus.empty
            withContext(Dispatchers.Main) {
                DbContext.data.update(redPacketData).blockingGet()
            }
            throw ClaimTooLateError()
        }
        val currentId = redPacketData.passwords[availability.component3().toInt()]
        val data = contract.claim(
                redPacketId,
                currentId,
                credentials.address.removePrefix("0x"),
                credentials.address.removePrefix("0x").sha3Hex()
        ).encodeFunctionCall()
        val nonce = web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING).sendAsync().await().let {
            it.transactionCount
        }
        val rawTransaction = RawTransaction.createTransaction(nonce, defaultGasPrice, defaultGasLimit, contract.contractAddress, data)
        val signedRawTransaction = TransactionEncoder.signMessage(rawTransaction, ethChainID, credentials)
        val transaction = web3j.ethSendRawTransaction(Numeric.toHexString(signedRawTransaction)).sendAsync().await()
        if (transaction.transactionHash == null) {
            throw TransactionException(transaction.error.message)
        }
        redPacketData.claimTransactionHash = transaction.transactionHash
        redPacketData.claimNonce = nonce.toInt()
        redPacketData.claimFunctionCall = data
        redPacketData.status = RedPacketStatus.claimPending
        withContext(Dispatchers.Main) {
            DbContext.data.update(redPacketData).blockingGet()
        }
        return redPacketData
    }
}

class ClaimTooLateError : Error {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) : super(message, cause, enableSuppression, writableStackTrace)
}