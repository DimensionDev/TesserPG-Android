package com.sujitech.tessercubecore.viewmodel.wallet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sujitech.tessercubecore.common.extension.await
import com.sujitech.tessercubecore.common.wallet.*
import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.contracts.generated.IERC20
import com.sujitech.tessercubecore.data.*
import com.sujitech.tessercubecore.wallet.BalanceUpdater
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.crypto.Hash
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.exceptions.TransactionException
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow

class SendRedPacketViewModel : ViewModel() {

    private var subscription: Disposable?
    val updateTime = MutableLiveData<Long>()
    val token = MutableLiveData<WalletToken>()

    val wallets by lazy {
        ArrayList(DbContext.data.select(WalletData::class).get().toList())
    }

    init {
        subscription = DbContext.data.select(WalletData::class).get().observableResult().subscribe {
            wallets.clear()
            wallets.addAll(it)
            updateTime.value = System.currentTimeMillis()
        }
        DbContext.data.select(WalletData::class).get().forEach {
            BalanceUpdater.update(it)
        }
    }

    /**
     * Committing red packet
     *
     * @param amount In ETH
     * @param shares
     * @param isRandom
     * @param senderName
     * @param senderMessage
     * @param walletPassword
     * @param walletMnemonic
     */
    suspend fun commit(
            amount: BigDecimal,
            shares: Long,
            isRandom: Boolean,
            senderName: String,
            senderMessage: String,
            walletPassword: String,
            walletMnemonic: String
    ): RedPacketData {
        val credentials = WalletUtils.loadBip39Credentials(walletPassword, walletMnemonic)
        val contractGasProvider = getDefaultGasProvider()
        val uuid = UUID.randomUUID().toString()
//        val uuids = (0 until shares).map {
//            UUID.randomUUID().toString()
//        }
        val type = token.value?.token?.symbol ?: "ETH"
        val web3j = currentEthNetworkType.web3j
        val result = if (type == "ETH") {
            val contract = HappyRedPacket.load(
                    currentEthNetworkType.defaultContractAddress,
                    web3j,
                    credentials,
                    contractGasProvider)
            val weiValue = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger()
            val data = contract.create_red_packet(
                    uuid.sha3Hex(),
                    shares.toBigInteger(),
//                    uuids.map { it.sha3Hex() }.first(),
//                    uuids.count().toBigInteger(),
                    isRandom,
                    defaultRedPacketDuration.toBigInteger(),//TODO
                    Hash.sha3("seed".toByteArray()),
                    senderMessage,
                    senderName,
                    0.toBigInteger(),
                    currentEthNetworkType.defaultContractAddress,
                    weiValue,
                    weiValue
            ).encodeFunctionCall()
            val nonce = web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING).sendAsync().await().let {
                it.transactionCount
            }
            val rawTransaction = RawTransaction.createTransaction(nonce, defaultGasPrice, defaultGasLimit, contract.contractAddress, weiValue, data)
            val signedRawTransaction = TransactionEncoder.signMessage(rawTransaction, currentEthNetworkType.ethChainID, credentials)
            val transaction = web3j.ethSendRawTransaction(Numeric.toHexString(signedRawTransaction)).sendAsync().await()
            if (transaction.transactionHash == null) {
                throw TransactionException(transaction.error.message)
            }
            val redPacketData = RedPacketDataEntity().apply {
                this.blockCreationTime = System.currentTimeMillis() / 1000
                this.contractAddress = currentEthNetworkType.defaultContractAddress
                this.duration = defaultRedPacketDuration
                this.isRandom = isRandom
                this.sendMessage = senderMessage
                this.senderAddress = credentials.address
                this.senderName = senderName
                this.network = currentEthNetworkType
                this.sendTotal = weiValue.toBigDecimal() //TODO
                this.password = uuid
                this.shares = shares.toInt()
//                this.uuids = uuids.joinToString(";")
                this.contractVersion = defaultContractVersion
                this.aesVersion = defaultAESVersion
                this.status = RedPacketStatus.pending
                this.tokenType = RedPacketTokenType.ETH
                //TODO: split creation step
                this.creationTransactionHash = transaction.transactionHash
                this.createNonce = nonce.toInt()
                this.creationFunctionCall = data
            }
            withContext(Dispatchers.Main) {
                DbContext.data.insert(redPacketData).blockingGet()
            }
            redPacketData
        } else {
            val tokenValue = token.value?.token ?: throw Error()
            val sendValue = amount * 10.0.pow(tokenValue.decimals).toBigDecimal()
            val erc20 = IERC20.load(tokenValue.address, web3j, credentials, contractGasProvider)
            val data = erc20.approve(currentEthNetworkType.defaultContractAddress, sendValue.toBigInteger()).encodeFunctionCall()
            val nonce = web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING).sendAsync().await().let {
                it.transactionCount
            }
            val rawTransaction = RawTransaction.createTransaction(nonce, defaultGasPrice, defaultGasLimit, erc20.contractAddress, data)
            val signedRawTransaction = TransactionEncoder.signMessage(rawTransaction, currentEthNetworkType.ethChainID, credentials)
            val transaction = web3j.ethSendRawTransaction(Numeric.toHexString(signedRawTransaction)).sendAsync().await()
            if (transaction.transactionHash == null) {
                throw TransactionException(transaction.error.message)
            }
            val redPacketData = RedPacketDataEntity().apply {
                this.blockCreationTime = System.currentTimeMillis() / 1000
                this.contractAddress = currentEthNetworkType.defaultContractAddress
                this.duration = defaultRedPacketDuration
                this.isRandom = isRandom
                this.network = currentEthNetworkType
                this.sendMessage = senderMessage
                this.senderAddress = credentials.address
                this.senderName = senderName
                this.sendTotal = sendValue //TODO
                this.password = uuid
                this.shares = shares.toInt()
//                this.uuids = uuids.joinToString(";")
                this.contractVersion = defaultContractVersion
                this.aesVersion = defaultAESVersion
                this.status = RedPacketStatus.pending
                this.tokenType = RedPacketTokenType.ERC20
                //TODO: split creation step
                this.erC20Token = tokenValue
                this.erc20ApproveTransactionHash = transaction.transactionHash
                this.erc20ApproveNonce = nonce.toInt()
                this.erc20ApproveFunctionCall = data
            }
            withContext(Dispatchers.Main) {
                DbContext.data.insert(redPacketData).blockingGet()
            }
            redPacketData
        }
        web3j.shutdown()
        return result
    }

    override fun onCleared() {
        super.onCleared()
        subscription?.dispose()
    }
}