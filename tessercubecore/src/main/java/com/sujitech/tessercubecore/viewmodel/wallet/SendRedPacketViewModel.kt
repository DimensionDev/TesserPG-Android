package com.sujitech.tessercubecore.viewmodel.wallet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sujitech.tessercubecore.common.extension.await
import com.sujitech.tessercubecore.common.wallet.*
import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.contracts.generated.IERC20
import com.sujitech.tessercubecore.data.*
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

class SendRedPacketViewModel : ViewModel() {

    val token = MutableLiveData<ERC20Token>()

    val wallets = MutableLiveData<List<WalletData>>(DbContext.data.select(WalletData::class).get().toList())

    //TODO: update wallet && token balance

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
        val uuids = (0 until shares).map {
            UUID.randomUUID().toString()
        }
        val type = token.value?.symbol ?: "ETH"
        if (type == "ETH") {
            val contract = HappyRedPacket.load(
                    redPacketContractAddress,
                    web3j,
                    credentials,
                    contractGasProvider)
            val weiValue = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger()
            val data = contract.create_red_packet(
                    uuids.map { it.sha3Hex() },
                    isRandom,
                    defaultRedPacketDuration.toBigInteger(),//TODO
                    Hash.sha3("seed".toByteArray()),
                    senderMessage,
                    senderName,
                    0.toBigInteger(),
                    redPacketContractAddress,
                    weiValue,
                    weiValue
            ).encodeFunctionCall()
            val nonce = web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING).sendAsync().await().let {
                it.transactionCount
            }
            val rawTransaction = RawTransaction.createTransaction(nonce, defaultGasPrice, defaultGasLimit, contract.contractAddress, weiValue, data)
            val signedRawTransaction = TransactionEncoder.signMessage(rawTransaction, ethChainID, credentials)
            val transaction = web3j.ethSendRawTransaction(Numeric.toHexString(signedRawTransaction)).sendAsync().await()
            if (transaction.transactionHash == null) {
                throw TransactionException(transaction.error.message)
            }
            val redPacketData = RedPacketDataEntity().apply {
                this.blockCreationTime = System.currentTimeMillis() / 1000
                this.contractAddress = redPacketContractAddress
                this.duration = defaultRedPacketDuration
                this.isRandom = isRandom
                this.sendMessage = senderMessage
                this.senderAddress = credentials.address
                this.senderName = senderName
                this.sendTotal = weiValue.toBigDecimal() //TODO
                this.uuids = uuids.joinToString(";")
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
            return redPacketData
        } else {
            val tokenValue = token.value ?: throw Error()
            val sendValue = amount
            val erc20 = IERC20.load(tokenValue.address, web3j, credentials, contractGasProvider)
            val data = erc20.approve(redPacketContractAddress, sendValue.toBigInteger()).encodeFunctionCall()
            val nonce = web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING).sendAsync().await().let {
                it.transactionCount
            }
            val rawTransaction = RawTransaction.createTransaction(nonce, defaultGasPrice, defaultGasLimit, erc20.contractAddress, data)
            val signedRawTransaction = TransactionEncoder.signMessage(rawTransaction, ethChainID, credentials)
            val transaction = web3j.ethSendRawTransaction(Numeric.toHexString(signedRawTransaction)).sendAsync().await()
            if (transaction.transactionHash == null) {
                throw TransactionException(transaction.error.message)
            }
            val redPacketData = RedPacketDataEntity().apply {
                this.blockCreationTime = System.currentTimeMillis() / 1000
                this.contractAddress = redPacketContractAddress
                this.duration = defaultRedPacketDuration
                this.isRandom = isRandom
                this.sendMessage = senderMessage
                this.senderAddress = credentials.address
                this.senderName = senderName
                this.sendTotal = sendValue //TODO
                this.uuids = uuids.joinToString(";")
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
            return redPacketData
        }
    }
}