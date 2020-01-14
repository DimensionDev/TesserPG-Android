package com.sujitech.tessercubecore.viewmodel.wallet

import androidx.lifecycle.ViewModel
import com.sujitech.tessercubecore.appContext
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.extension.await
import com.sujitech.tessercubecore.common.extension.hexStringToByteArray
import com.sujitech.tessercubecore.common.wallet.*
import com.sujitech.tessercubecore.contracts.generated.ERC20Detailed
import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.data.*
import io.requery.kotlin.eq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.web3j.crypto.Hash
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
        checkIsCustomToken(redPacketData)
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
        val currentId = redPacketData.passwords[availability.component4().toInt()]
        val data = contract.claim(
                redPacketId,
                currentId,
                credentials.address.removePrefix("0x"),
                Hash.sha3(credentials.address.removePrefix("0x").hexStringToByteArray())
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
        redPacketData.claimAddress = credentials.address
        redPacketData.claimTransactionHash = transaction.transactionHash
        redPacketData.claimNonce = nonce.toInt()
        redPacketData.claimFunctionCall = data
        redPacketData.status = RedPacketStatus.claimPending
        withContext(Dispatchers.Main) {
            DbContext.data.update(redPacketData).blockingGet()
        }
        return redPacketData
    }

    private suspend fun checkIsCustomToken(redPacketData: RedPacketData) {
        val data = Json.nonstrict.parse(RedPacketRawPayload.serializer(), redPacketData.rawPayload!!)
        val erC20TokenData = data.token
        if (erC20TokenData != null) {
            val erC20Token = DbContext.data.select(ERC20Token::class).where(ERC20Token::address eq erC20TokenData.address).get().firstOrNull()
            if (erC20Token == null) {
                val wallet = DbContext.data.select(WalletData::class).get().firstOrNull()
                if (wallet != null) {
                    val mnemonic = UserPasswordStorage.get(appContext, wallet.mnemonicId)
                    val password = UserPasswordStorage.get(appContext, wallet.passwordId)
                    val credential = WalletUtils.loadBip39Credentials(password, mnemonic)
                    val erc20 = ERC20Detailed.load(erC20TokenData.address, web3j, credential, getDefaultGasProvider())
                    val name = erc20.name().sendAsync().await()
                    val symbol = erc20.symbol().sendAsync().await()
                    val decimals = erc20.decimals().sendAsync().await()
                    val tokenEntity = ERC20TokenEntity().apply {
                        address = erC20TokenData.address
                        this.name = name
                        this.symbol = symbol
                        this.decimals = decimals.toInt()
                        this.isUserDefine = true
                        this.network = currentEthNetworkType
                    }
                    withContext(Dispatchers.Main) {
                        redPacketData.erC20Token = DbContext.data.insert(tokenEntity).blockingGet()
                    }
                }
            }
        }
    }
}

class ClaimTooLateError : Error {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) : super(message, cause, enableSuppression, writableStackTrace)
}