package com.sujitech.tessercubecore.viewmodel.wallet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sujitech.tessercubecore.common.wallet.*
import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.crypto.Hash
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.util.*

class SendRedPacketViewModel : ViewModel() {


    val wallets = MutableLiveData<List<WalletData>>(DbContext.data.select(WalletData::class).get().toList())

    fun refreshWalletBalance() {
        viewModelScope.launch(Dispatchers.IO) {
            val web3j = createWeb3j()
            wallets.value?.let { wallets ->
                for (wallet in wallets) {
                    val balance = web3j.ethGetBalance(wallet.address, DefaultBlockParameterName.LATEST).send()
                    wallet.balance = balance.balance.toBigDecimal()
                    withContext(Dispatchers.Main) {
                        DbContext.data.update(wallet).blockingGet()
                    }
                }
            }
            withContext(Dispatchers.Main) {
                wallets.value = DbContext.data.select(WalletData::class).get().toList()
            }
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
        val web3j = createWeb3j()
        val credentials = WalletUtils.loadBip39Credentials(walletPassword, walletMnemonic)
        val contractGasProvider = getDefaultGasProvider()
        val contract = HappyRedPacket.load(
                redPacketContractAddress,
                web3j,
                credentials,
                contractGasProvider)
        val uuids = (0 until shares).map {
            UUID.randomUUID().toString()
        }
        val weiValue = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger()
        val data = contract.create_red_packet(
                uuids.map { it.sha3Hex() },
                isRandom,
                defaultRedPacketDuration.toBigInteger(),//TODO
                Hash.sha3("seed".toByteArray()),
                senderMessage,
                senderName,
                weiValue
        ).encodeFunctionCall()
        val nonce = web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING).send().let {
            it.transactionCount
        }
        val rawTransaction = RawTransaction.createTransaction(nonce, defaultGasPrice, defaultGasLimit, contract.contractAddress, weiValue, data)
        val signedRawTransaction = TransactionEncoder.signMessage(rawTransaction, ethChainID, credentials)
        val transaction = web3j.ethSendRawTransaction(Numeric.toHexString(signedRawTransaction)).send()
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
            //TODO: split creation step
            this.creationTransactionHash = transaction.transactionHash
            this.createNonce = nonce.toInt()
        }
        withContext(Dispatchers.Main) {
            DbContext.data.insert(redPacketData).blockingGet()
        }
        return redPacketData
    }
}