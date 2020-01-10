package com.sujitech.tessercubecore.wallet

import com.sujitech.tessercubecore.appContext
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.extension.await
import com.sujitech.tessercubecore.common.extension.toHexString
import com.sujitech.tessercubecore.common.extension.toJson
import com.sujitech.tessercubecore.common.wallet.*
import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.contracts.generated.IERC20
import com.sujitech.tessercubecore.data.*
import org.web3j.crypto.Hash
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.exceptions.TransactionException
import org.web3j.utils.Numeric

class PendingRedPacketHandler : IRedPacketHandler {

    private val erc20Handler by lazy {
        ERC20PendingRedPacketHandler()
    }

    private val ethHandler by lazy {
        ETHPendingRedPacketHandler()
    }

    override suspend fun processingRedPacket(redPacketData: RedPacketData, contract: HappyRedPacket) {
        if (redPacketData.creationTransactionHash == null && redPacketData.erC20Token != null) {
            erc20Handler.processingRedPacket(redPacketData, contract)
        } else {
            ethHandler.processingRedPacket(redPacketData, contract)
        }
    }
}

class ERC20PendingRedPacketHandler : RedPacketHandler() {
    override fun getNonce(redPacketData: RedPacketData): Int {
        return redPacketData.erc20ApproveNonce!!
    }

    override fun getFunctionCall(redPacketData: RedPacketData): String {
        return redPacketData.erc20ApproveFunctionCall!!
    }

    override fun getTransactionHash(redPacketData: RedPacketData): String {
        return redPacketData.erc20ApproveTransactionHash!!
    }

    override suspend fun onSuccess(contract: HappyRedPacket, redPacketData: RedPacketData, transactionReceipt: TransactionReceipt) {
        val wallet = DbContext.data.select(WalletData::class).get().firstOrNull() ?: return
        val mnemonic = UserPasswordStorage.get(appContext, wallet.mnemonicId)
        val password = UserPasswordStorage.get(appContext, wallet.passwordId)
        val credential = WalletUtils.loadBip39Credentials(password, mnemonic)
        val erc20 = IERC20.load(redPacketData.erC20Token!!.address, web3j, credential, getDefaultGasProvider())
        val event = erc20.getApprovalEvents(transactionReceipt).first()
        redPacketData.erc20ApproveResult = event.value.toBigDecimal()
        val data = contract.create_red_packet(
                redPacketData.passwords.map { it.sha3Hex() },
                redPacketData.isRandom,
                defaultRedPacketDuration.toBigInteger(),//TODO
                Hash.sha3("seed".toByteArray()),
                redPacketData.sendMessage,
                redPacketData.senderName,
                1.toBigInteger(),
                redPacketData.erC20Token!!.address,
                redPacketData.sendTotal.toBigInteger(),
                redPacketData.sendTotal.toBigInteger()
        ).encodeFunctionCall()
        val nonce = web3j.ethGetTransactionCount(credential.address, DefaultBlockParameterName.PENDING).sendAsync().await().let {
            it.transactionCount
        }
        val rawTransaction = RawTransaction.createTransaction(nonce, defaultGasPrice, defaultGasLimit, contract.contractAddress, data)
        val signedRawTransaction = TransactionEncoder.signMessage(rawTransaction, ethChainID, credential)
        val transaction = web3j.ethSendRawTransaction(Numeric.toHexString(signedRawTransaction)).sendAsync().await()
        if (transaction.transactionHash == null) {
            throw TransactionException(transaction.error.message)
        }
        redPacketData.creationTransactionHash = transaction.transactionHash
        redPacketData.createNonce = nonce.toInt()
        redPacketData.creationFunctionCall = data
        saveResult(redPacketData)
        RedPacketUpdater.put(redPacketData)
    }

    override suspend fun onRevert(redPacketData: RedPacketData, revertReason: String?) {
        redPacketData.status = RedPacketStatus.fail
        redPacketData.failReason = revertReason
        saveResult(redPacketData)
    }

}

class ETHPendingRedPacketHandler : RedPacketHandler() {
    override fun getNonce(redPacketData: RedPacketData): Int {
        return redPacketData.createNonce!!
    }

    override fun getFunctionCall(redPacketData: RedPacketData): String {
        return redPacketData.creationFunctionCall!!
    }

    override fun getTransactionHash(redPacketData: RedPacketData): String {
        return redPacketData.creationTransactionHash!!
    }

    override suspend fun onSuccess(contract: HappyRedPacket, redPacketData: RedPacketData, transactionReceipt: TransactionReceipt) {
        val event = contract.getCreationSuccessEvents(transactionReceipt).first()
        redPacketData.redPacketId = event.id.toHexString()
        redPacketData.status = RedPacketStatus.normal
        redPacketData.rawPayload = redPacketData.toRawPayload().toJson(RedPacketRawPayload.serializer())
        redPacketData.encPayload = RedPacketPayloadHelper.generate(redPacketData)
        saveResult(redPacketData)
    }

    override suspend fun onRevert(redPacketData: RedPacketData, revertReason: String?) {
        redPacketData.status = RedPacketStatus.fail
        redPacketData.failReason = revertReason
        saveResult(redPacketData)
    }
}