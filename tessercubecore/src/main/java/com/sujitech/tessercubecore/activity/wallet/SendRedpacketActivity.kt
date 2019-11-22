package com.sujitech.tessercubecore.activity.wallet

import android.app.ProgressDialog
import android.os.Bundle
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.adapter.FragmentAdapter
import com.sujitech.tessercubecore.common.extension.applyMessageData
import com.sujitech.tessercubecore.common.extension.toast
import com.sujitech.tessercubecore.contracts.generated.RedPacket
import com.sujitech.tessercubecore.data.ContactData
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.MessageDataEntity
import com.sujitech.tessercubecore.fragment.wallet.ReceiverSelectFragment
import com.sujitech.tessercubecore.fragment.wallet.RedPacketInfoFragment
import io.requery.kotlin.eq
import kotlinx.android.synthetic.main.activity_send_redpacket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.tlaster.kotlinpgp.KotlinPGP
import moe.tlaster.kotlinpgp.data.EncryptParameter
import moe.tlaster.kotlinpgp.data.PublicKeyData
import org.web3j.crypto.Hash
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import java.util.*

class SendRedpacketActivity : BaseActivity() {


    private val infoFragment by lazy {
        RedPacketInfoFragment().apply {
            back = {
                finish()
            }
            next = {
                this@SendRedpacketActivity.viewPager.currentItem = this@SendRedpacketActivity.viewPager.currentItem + 1
            }
        }
    }

    private val receiverSelectFragment by lazy {
        ReceiverSelectFragment().apply {
            back = {
                this@SendRedpacketActivity.viewPager.currentItem = this@SendRedpacketActivity.viewPager.currentItem - 1
            }
            next = {
                commitRedPacket()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_redpacket)
        viewPager.adapter = FragmentAdapter(listOf(
                infoFragment, receiverSelectFragment
        ), supportFragmentManager)
    }

    private fun commitRedPacket() {
        //TODO
        val dialog = ProgressDialog.show(this, "Sending", "Sending...")
        val data = infoFragment.getRedPacketData()
        val receivers = receiverSelectFragment.getSelectedReceiver()
        GlobalScope.launch {

            val web3j = Web3j.build(HttpService("<TODO>"))  // TODO;
            val walletPassword = UserPasswordStorage.get(this@SendRedpacketActivity, data.wallet.passwordId)
            val walletMnemonic = UserPasswordStorage.get(this@SendRedpacketActivity, data.wallet.mnemonicId)
            val credentials = WalletUtils.loadBip39Credentials(walletPassword, walletMnemonic)
            val contractGasProvider = DefaultGasProvider()
            val uuids = (0 until data.shares).map {
                UUID.randomUUID().toString()
            }

            try {
                val contract = RedPacket.deploy(
                        web3j,
                        credentials,
                        contractGasProvider,
                        Convert.toWei(10.toBigDecimal(), Convert.Unit.FINNEY).toBigInteger(),
                        uuids.map {
                            Hash.sha3String(it).removePrefix("0x").hexStringToByteArray()
                        },
                        true,
                        (System.currentTimeMillis() / 1000 + 60 * 60).toBigInteger()
                ).send()

                val payload = generatePayload(contract.contractAddress, uuids)

                val result = KotlinPGP.encrypt(EncryptParameter(
                        message = payload,
                        publicKey = receivers.map { PublicKeyData(it.pubKeyContent) } + DbContext.data.select(ContactData::class).where(ContactData::isUserKey eq true).get().map { PublicKeyData(it.pubKeyContent, true) }
                ))
                runOnUiThread {
                    MessageDataEntity().also {
                        it.applyMessageData(payload, result, null, receivers)
                        DbContext.data.insert(it).blockingGet()
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                runOnUiThread {
                    //TODO
                    toast("error")
                }
            }
            runOnUiThread {
                dialog.hide()
            }
        }

    }

    private fun generatePayload(contractAddress: String, uuids: List<String>): String {
        val result = "-----BEGIN RED PACKET-----" +
                System.lineSeparator() +
                contractAddress +
                System.lineSeparator() +
                uuids.joinToString { System.lineSeparator() } +
                System.lineSeparator() +
                "-----END RED PACKET-----" + System.lineSeparator()
        return result
    }
}

fun String.hexStringToByteArray() = ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }