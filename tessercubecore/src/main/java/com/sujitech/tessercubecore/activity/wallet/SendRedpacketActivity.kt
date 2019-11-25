package com.sujitech.tessercubecore.activity.wallet

import android.os.Bundle
import androidx.activity.viewModels
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.adapter.FragmentAdapter
import com.sujitech.tessercubecore.common.extension.task
import com.sujitech.tessercubecore.common.extension.toast
import com.sujitech.tessercubecore.fragment.wallet.ReceiverSelectFragment
import com.sujitech.tessercubecore.fragment.wallet.RedPacketInfoFragment
import com.sujitech.tessercubecore.viewModel.wallet.SendRedPacketViewModel
import kotlinx.android.synthetic.main.activity_send_redpacket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.protocol.exceptions.TransactionException

class SendRedpacketActivity : BaseActivity() {

    private val viewModel by viewModels<SendRedPacketViewModel>()

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
        val data = infoFragment.getRedPacketData()
        val receivers = receiverSelectFragment.getSelectedReceiver()
        val walletPassword = UserPasswordStorage.get(this, data.wallet.passwordId)
        val walletMnemonic = UserPasswordStorage.get(this, data.wallet.mnemonicId)
        task(Dispatchers.IO) {
            kotlin.runCatching {
                viewModel.commitRedPacket(data, receivers, walletPassword!!, walletMnemonic!!)
            }.onFailure {
                it.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (it is RuntimeException || it is TransactionException) {
                        toast(it.message ?: "Send Error")
                    } else {
                        toast("Send Error")
                    }
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }

}

