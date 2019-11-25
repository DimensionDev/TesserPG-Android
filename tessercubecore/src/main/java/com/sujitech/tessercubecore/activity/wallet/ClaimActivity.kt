package com.sujitech.tessercubecore.activity.wallet

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.viewModels
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.extension.task
import com.sujitech.tessercubecore.common.extension.toast
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.MessageData
import com.sujitech.tessercubecore.data.RedPacketState
import com.sujitech.tessercubecore.data.WalletData
import com.sujitech.tessercubecore.viewModel.wallet.ClaimFailureError
import com.sujitech.tessercubecore.viewModel.wallet.ClaimViewModel
import kotlinx.android.synthetic.main.activity_claim.*
import kotlinx.coroutines.Dispatchers
import org.web3j.protocol.exceptions.TransactionException

class ClaimActivity : BaseActivity() {

    private val data by lazy {
        //TODO
        DbContext.data.findByKey(MessageData::class, intent.getParcelableExtra<MessageData>("data").dataId).blockingGet()
    }

    private val viewModel by viewModels<ClaimViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_claim)
        viewModel.data = data
        red_packet_card.data = data
        val wallets = DbContext.data.select(WalletData::class).get().toList()
        wallet_spinner.adapter = object : ArrayAdapter<WalletData>(this, android.R.layout.simple_spinner_dropdown_item, wallets) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val spinnerView = convertView
                        ?: layoutInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)
                val item = getItem(position)
                spinnerView.findViewById<TextView>(android.R.id.text1).text = item?.address
                        ?: ""
                return spinnerView
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return getView(position, convertView, parent)
            }
        }

        open_red_packet.setOnClickListener {
            task(Dispatchers.IO) {
                (wallet_spinner.selectedItem as? WalletData)?.let {
                    val walletPassword = UserPasswordStorage.get(this@ClaimActivity, it.passwordId)
                    val walletMnemonic = UserPasswordStorage.get(this@ClaimActivity, it.mnemonicId)
                    try {
                        val value = viewModel.openRedPacket(walletPassword!!, walletMnemonic!!)
                        if (value != null) {
                            data?.redPacketData?.price = value.toBigDecimal()
                            data?.redPacketData?.state = RedPacketState.claimed
                            runOnUiThread {
                                data?.redPacketData?.let {
                                    DbContext.data.update(it).blockingGet()
                                }
                                toast("Claim success")
                            }
                        }
                        finish()
                    } catch (e: ClaimFailureError) {
                        data?.redPacketData?.state = RedPacketState.claimLate
                        runOnUiThread {
                            data?.redPacketData?.let {
                                DbContext.data.update(it).blockingGet()
                            }
                            toast("Claim failure")
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        data?.redPacketData?.state = RedPacketState.claimFailed
                        runOnUiThread {
                            data?.redPacketData?.let {
                                DbContext.data.update(it).blockingGet()
                            }
                            if (e is RuntimeException || e is TransactionException) {
                                toast(e.message ?: "Claim Error")
                            } else {
                                toast("Claim Error")
                            }
                        }
                    }
                }
            }
        }
    }
}
