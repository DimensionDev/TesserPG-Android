package com.sujitech.tessercubecore.viewmodel.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sujitech.tessercubecore.appContext
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.collection.ObservableCollection
import com.sujitech.tessercubecore.common.extension.await
import com.sujitech.tessercubecore.common.extension.hexStringToByteArray
import com.sujitech.tessercubecore.common.wallet.*
import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.RedPacketData
import com.sujitech.tessercubecore.data.RedPacketStatus
import com.sujitech.tessercubecore.data.WalletData
import io.requery.kotlin.eq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.exceptions.TransactionException
import org.web3j.utils.Numeric
import java.math.BigDecimal

class RedPacketDetailViewModel : ViewModel() {
    val claimers = ObservableCollection<RedPacketClaimerData>()

    fun loadClaimerList(data: RedPacketData?) {
        if (data == null) {
            return
        }
        val wallet = DbContext.data.select(WalletData::class).get().firstOrNull() ?: return
        val walletPassword = UserPasswordStorage.get(appContext, wallet.passwordId)
        val walletMnemonic = UserPasswordStorage.get(appContext, wallet.mnemonicId)
        viewModelScope.launch {
            val credentials = WalletUtils.loadBip39Credentials(walletPassword, walletMnemonic)
            val contractGasProvider = getDefaultGasProvider()
            val web3j = data.network.web3j
            val contract = HappyRedPacket.load(
                    data.contractAddress,
                    web3j,
                    credentials,
                    contractGasProvider)
            val result = contract.check_claimed_list(data.redPacketId!!.hexStringToByteArray()).sendAsync().await()
            if (result.component1().count() != result.component2().count()) {
                return@launch
            }
            if (!result.component1().any()) {
                return@launch
            }
            result.component1().mapIndexed { index, bigInteger ->
                RedPacketClaimerData(
                        result.component2()[index],
                        "", //TODO
                        bigInteger.toBigDecimal()
                )
            }.let {
                claimers.addAll(it)
            }
            web3j.shutdown()
        }
    }

    suspend fun refund(data: RedPacketData?): RedPacketData? {
        if (data == null) {
            return null
        }
        val wallet = DbContext.data.select(WalletData::class).where(WalletData::address eq data.senderAddress).get().firstOrNull() ?: return null
        val walletPassword = UserPasswordStorage.get(appContext, wallet.passwordId)
        val walletMnemonic = UserPasswordStorage.get(appContext, wallet.mnemonicId)
        val credentials = WalletUtils.loadBip39Credentials(walletPassword, walletMnemonic)
        val contractGasProvider = getDefaultGasProvider()
        val web3j = data.network.web3j
        val contract = HappyRedPacket.load(
                data.contractAddress,
                web3j,
                credentials,
                contractGasProvider)
        val call = contract.refund(data.redPacketId!!.hexStringToByteArray()).encodeFunctionCall()
        val nonce = web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING).sendAsync().await().let {
            it.transactionCount
        }
        val rawTransaction = RawTransaction.createTransaction(nonce, defaultGasPrice, defaultGasLimit, contract.contractAddress, call)
        val signedRawTransaction = TransactionEncoder.signMessage(rawTransaction, data.network.ethChainID, credentials)
        val transaction = web3j.ethSendRawTransaction(Numeric.toHexString(signedRawTransaction)).sendAsync().await()
        if (transaction.transactionHash == null) {
            throw TransactionException(transaction.error.message)
        }
        data.refundTransactionHash = transaction.transactionHash
        data.refundNonce = nonce.toInt()
        data.refundFunctionCall = call
        data.status = RedPacketStatus.refundPending
        withContext(Dispatchers.Main) {
            DbContext.data.update(data).blockingGet()
        }
        web3j.shutdown()
        return data
    }
}

data class RedPacketClaimerData(
        val address: String,
        val name: String,
        val amount: BigDecimal
)