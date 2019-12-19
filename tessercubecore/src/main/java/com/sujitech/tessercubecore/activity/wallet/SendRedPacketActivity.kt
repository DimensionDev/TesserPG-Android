package com.sujitech.tessercubecore.activity.wallet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.Settings
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.extension.formatWei
import com.sujitech.tessercubecore.common.extension.task
import com.sujitech.tessercubecore.common.extension.toActivity
import com.sujitech.tessercubecore.data.WalletData
import com.sujitech.tessercubecore.viewmodel.wallet.SendRedPacketViewModel
import kotlinx.android.synthetic.main.activity_send_red_packet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendRedPacketActivity : BaseActivity() {

    private val walletSpinnerAdapter by lazy {
        object : ArrayAdapter<WalletData>(this, android.R.layout.simple_spinner_dropdown_item, viewModel.wallets.value!!) {
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
    private val viewModel by viewModels<SendRedPacketViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_red_packet)

        wallet_spinner.adapter = walletSpinnerAdapter

        wallet_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.wallets.value?.get(position)?.balance?.let {
                    wallet_eth.text = it.formatWei()
                }
            }

        }
        commit_button.setOnClickListener {
            commit()
        }
        viewModel.wallets.observe(this, Observer {
            it[wallet_spinner.selectedItemPosition].balance?.let {
                wallet_eth.text = it.formatWei()
            }
        })
        name_input.setText(Settings.get("red_packet_sender_name", ""))
        viewModel.refreshWalletBalance()
    }

    private fun commit() {
        when {
            amount_input.text.isNullOrEmpty() || amount_input.text.toString().toBigDecimal() == 0.toBigDecimal() -> {
                amount_input.error = "Please input amount"
            }
            shares_input.text.isNullOrEmpty() || shares_input.text.toString().toInt() == 0 -> {
                shares_input.error = "Please input shares count"
            }
            amount_input.text.toString().toBigDecimal() < 0.002.toBigDecimal() * shares_input.text.toString().toBigDecimal() -> {
                amount_input.error = "Amount must above ${0.002 * shares_input.text.toString().toDouble()} ETH"
            }
            name_input.text.isNullOrEmpty() -> {
                name_input.error = "Please set your name"
            }
            else -> {
                task {
                    val wallet = wallet_spinner.selectedItem as WalletData
                    val walletPassword = UserPasswordStorage.get(this@SendRedPacketActivity, wallet.passwordId)!!
                    val walletMnemonic = UserPasswordStorage.get(this@SendRedPacketActivity, wallet.mnemonicId)!!
                    val data = viewModel.commit(
                            amount_input.text.toString().toBigDecimal(),
                            shares_input.text.toString().toLong(),
                            tab_layout.selectedTabPosition == 1, //TODO
                            name_input.text.toString(),
                            message_input.text.toString(),
                            walletPassword,
                            walletMnemonic
                    )
                    withContext(Dispatchers.Main) {
                        toActivity<SendRedPacketFinishActivity>(Intent().putExtra("data", data))
                        finish()
                    }
                }
            }
        }
    }
}

