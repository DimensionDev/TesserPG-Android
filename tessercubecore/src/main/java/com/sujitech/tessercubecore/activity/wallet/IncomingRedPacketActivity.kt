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
import com.sujitech.tessercubecore.data.RedPacketData
import com.sujitech.tessercubecore.data.WalletData
import com.sujitech.tessercubecore.viewmodel.wallet.ClaimTooLateError
import com.sujitech.tessercubecore.viewmodel.wallet.IncomingRedPacketViewModel
import kotlinx.android.synthetic.main.activity_incoming_red_packet.*

class IncomingRedPacketActivity : BaseActivity() {
    private val viewModel by viewModels<IncomingRedPacketViewModel>()

    private val walletSpinnerAdapter by lazy {
        object : ArrayAdapter<WalletData>(this, android.R.layout.simple_spinner_dropdown_item, viewModel.wallets) {
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
    }

    private val data by lazy {
        intent.getParcelableExtra<RedPacketData>("data")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_red_packet)
        red_packet.data = data
        wallet_spinner.adapter = walletSpinnerAdapter
        commit_button.setOnClickListener {
            openRedPacket()
        }
    }

    private fun openRedPacket() {
        val wallet = wallet_spinner.selectedItem as WalletData
        val walletPassword = UserPasswordStorage.get(this, wallet.passwordId)!!
        val walletMnemonic = UserPasswordStorage.get(this, wallet.mnemonicId)!!
        task {
            try {
                viewModel.openRedPacket(data, walletMnemonic, walletPassword)
                finish()
            } catch (e: ClaimTooLateError) {
                runOnUiThread {
                    toast("Too late!")
                }
                finish()
            }
        }
    }
}
