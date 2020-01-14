package com.sujitech.tessercubecore.wallet

import com.sujitech.tessercubecore.common.wallet.RedPacketRawPayload
import com.sujitech.tessercubecore.contracts.generated.HappyRedPacket
import com.sujitech.tessercubecore.data.*
import io.requery.kotlin.eq
import kotlinx.serialization.json.Json
import org.web3j.protocol.core.methods.response.TransactionReceipt

class ClaimPendingRedPacketHandler : RedPacketHandler() {
    override fun getNonce(redPacketData: RedPacketData): Int {
        return redPacketData.claimNonce!!
    }

    override fun getFunctionCall(redPacketData: RedPacketData): String {
        return redPacketData.claimFunctionCall!!
    }

    override fun getTransactionHash(redPacketData: RedPacketData): String {
        return redPacketData.claimTransactionHash!!
    }

    override suspend fun onSuccess(contract: HappyRedPacket, redPacketData: RedPacketData, transactionReceipt: TransactionReceipt) {
        val event = contract.getClaimSuccessEvents(transactionReceipt).first()
        redPacketData.claimAmount = event.claimed_value.toBigDecimal()
        redPacketData.status = RedPacketStatus.claimed
        if (redPacketData.tokenType == RedPacketTokenType.ERC20 && redPacketData.erC20Token == null) {
            // Incoming ERC20 token
            val data = Json.nonstrict.parse(RedPacketRawPayload.serializer(), redPacketData.rawPayload!!)
            data.token?.let { erC20TokenData ->
                val erC20Token = DbContext.data.select(ERC20Token::class).where(ERC20Token::address eq erC20TokenData.address).get().firstOrNull()
                val wallet = DbContext.data.select(WalletData::class).where(WalletData::address eq redPacketData.claimAddress!!).get().firstOrNull()
                if (wallet != null) { // should not be null
                    val count = DbContext.data.select(WalletToken::class).where(WalletToken::wallet eq wallet).get().count()
                    wallet.walletToken.add(WalletTokenEntity().apply {
                        this.orderIndex = count
                        this.token = erC20Token
                        this.wallet = wallet
                    })
                }
            }
        }
        saveResult(redPacketData)
    }

    override suspend fun onRevert(redPacketData: RedPacketData, revertReason: String?) {
        if (revertReason == null) {
            if (redPacketData.createNonce != null) {
                redPacketData.status = RedPacketStatus.normal
            } else {
                redPacketData.status = RedPacketStatus.incoming
            }
        } else {
            when (revertReason.take(3).toInt()) {
                3 -> redPacketData.status = RedPacketStatus.expired
                4 -> redPacketData.status = RedPacketStatus.empty
                5 -> redPacketData.status = RedPacketStatus.claimed //TODO
                6 -> redPacketData.status = RedPacketStatus.normal
                7 -> redPacketData.status = RedPacketStatus.normal
            }
        }
        saveResult(redPacketData)
    }
}