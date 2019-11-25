package com.sujitech.tessercubecore.viewModel.wallet

import androidx.lifecycle.ViewModel
import com.sujitech.tessercubecore.common.RedPacketUtils
import com.sujitech.tessercubecore.common.createWeb3j
import com.sujitech.tessercubecore.contracts.generated.RedPacket
import com.sujitech.tessercubecore.data.MessageData
import org.web3j.crypto.Hash
import org.web3j.crypto.WalletUtils
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger

class ClaimViewModel : ViewModel() {
    var data: MessageData? = null

    suspend fun openRedPacket(walletPassword: String, walletMnemonic: String): BigInteger? {
        return data?.let {
            RedPacketUtils.parse(it.content)
        }?.let { redPacketInfo ->
            val web3j = createWeb3j()
            val credentials = WalletUtils.loadBip39Credentials(walletPassword, walletMnemonic)
            val contractGasProvider = DefaultGasProvider()
            val contract = RedPacket.load(redPacketInfo.contractAddress, web3j, credentials, contractGasProvider)
            val availabilityResult = contract.check_availability().send()
            val uuid = redPacketInfo.uuids[redPacketInfo.uuids.count() - availabilityResult.component2().toInt()]
            val transactionReceipt = contract.claim(uuid, Hash.sha3("seed".toByteArray())).send()

            for (event in contract.getFailureEvents(transactionReceipt)) {
                throw ClaimFailureError()
            }

            for (event in contract.getClaimSuccessEvents(transactionReceipt)) {
                return@let event.claimed_value
            }
            null
        }
    }
}

class ClaimFailureError: Error {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) : super(message, cause, enableSuppression, writableStackTrace)
}