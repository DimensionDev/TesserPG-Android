package com.sujitech.tessercubecore.viewModel.wallet

import androidx.lifecycle.ViewModel
import com.sujitech.tessercubecore.common.RedPacketInfo
import com.sujitech.tessercubecore.common.createWeb3j
import com.sujitech.tessercubecore.common.extension.applyMessageData
import com.sujitech.tessercubecore.common.extension.hexStringToByteArray
import com.sujitech.tessercubecore.common.extension.toHexString
import com.sujitech.tessercubecore.contracts.generated.RedPacket
import com.sujitech.tessercubecore.data.ContactData
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.MessageDataEntity
import com.sujitech.tessercubecore.data.RedPacketDataEntity
import com.sujitech.tessercubecore.fragment.wallet.RedPacketData
import io.requery.kotlin.eq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.tlaster.kotlinpgp.KotlinPGP
import moe.tlaster.kotlinpgp.data.EncryptParameter
import moe.tlaster.kotlinpgp.data.PublicKeyData
import org.web3j.crypto.Hash
import org.web3j.crypto.WalletUtils
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import java.util.*

class SendRedPacketViewModel : ViewModel() {

    suspend fun commitRedPacket(data: RedPacketData, receivers: List<ContactData>, walletPassword: String, walletMnemonic: String) {
        val web3j = createWeb3j()
        val credentials = WalletUtils.loadBip39Credentials(walletPassword, walletMnemonic)
        val contractGasProvider = DefaultGasProvider()
        val uuids = (0 until data.shares).map {
            UUID.randomUUID().toString()
        }
        val contract = RedPacket.deploy(
                web3j,
                credentials,
                contractGasProvider,
                Convert.toWei(data.amount, Convert.Unit.ETHER).toBigInteger(),
                uuids.map {
                    Hash.sha3String(it).removePrefix("0x").hexStringToByteArray()
                },
                true,
                1.toBigInteger()
        ).send()

        val pubkeyRing = KotlinPGP.getPublicKeyRingFromString(data.sender.contactData!!.pubKeyContent)

        val payload = RedPacketInfo(
                pubkeyRing.publicKey.fingerprint.toHexString(),
                pubkeyRing.publicKey.userIDs.next(),
                contract.contractAddress,
                uuids).toString()

        val result = KotlinPGP.encrypt(EncryptParameter(
                message = payload,
                publicKey = receivers.map { PublicKeyData(it.pubKeyContent) } + DbContext.data.select(ContactData::class).where(ContactData::isUserKey eq true).get().map { PublicKeyData(it.pubKeyContent) }
        ))
        MessageDataEntity().also {
            it.redPacketData = RedPacketDataEntity().also { redPacketDataEntity ->
                redPacketDataEntity.fromMe = true
                redPacketDataEntity.price = data.amount
                redPacketDataEntity.shares = data.shares
            }
            it.applyMessageData(payload, result, null, receivers)
            withContext(Dispatchers.Main) {
                DbContext.data.insert(it).blockingGet()
            }
        }
    }
}