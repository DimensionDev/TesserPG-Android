package com.sujitech.tessercubecore.activity.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.Settings
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.extension.formatWei
import com.sujitech.tessercubecore.common.extension.task
import com.sujitech.tessercubecore.common.extension.toActivity
import com.sujitech.tessercubecore.data.*
import com.sujitech.tessercubecore.viewmodel.wallet.SendRedPacketViewModel
import com.sujitech.tessercubecore.wallet.BalanceUpdater
import io.requery.kotlin.eq
import kotlinx.android.synthetic.main.activity_send_red_packet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendRedPacketActivity : BaseActivity() {

    private val TOKEN_REQUEST_CODE = 789

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
    private val viewModel by viewModels<SendRedPacketViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_red_packet)

        wallet_spinner.adapter = walletSpinnerAdapter

        selected_token.text = "ETH" // TODO
        viewModel.token.observe(this, Observer {
            selected_token.text = it?.token?.symbol ?: "ETH"
            wallet_eth.text = "${it?.tokenBalance?.formatToken(selected_token.text != "ETH", it?.token?.decimals) ?: 0} ${it?.token?.symbol}"
        })
        selected_token.setOnClickListener {
            startActivityForResult(Intent(this, TokenSelectActivity::class.java)
                    .putExtra("data", wallet_spinner.selectedItem as WalletData), TOKEN_REQUEST_CODE)
        }

        wallet_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.wallets.get(position)?.let {
                    BalanceUpdater.update(it)
                }
                viewModel.wallets.get(position)?.currentBalance?.let {
                    wallet_eth.text = it.formatWei()
                    selected_token.text = "ETH" // TODO
                }
            }
        }
        commit_button.setOnClickListener {
            commit()
        }
        viewModel.updateTime.observe(this) {
            val wallet = viewModel.wallets.get(wallet_spinner.selectedItemPosition)
            if (viewModel.token.value?.token?.symbol ?: "ETH" == "ETH") {
                wallet_eth.text = wallet.currentBalance?.formatWei()
            } else {
                wallet_eth.text = wallet.walletToken.firstOrNull { it.token.address == viewModel.token.value?.token?.address }?.let {
                    "${it.tokenBalance?.formatToken(true, it.token.decimals)} ${it.token.symbol}"
                }
            }
        }
        name_input.setText(Settings.get("red_packet_sender_name", ""))
    }

    private fun commit() {
        when {
            amount_input.text.isNullOrEmpty() || amount_input.text.toString().toBigDecimal() == 0.toBigDecimal() -> {
                amount_input.error = "Please input amount"
            }
            shares_input.text.isNullOrEmpty() || shares_input.text.toString().toInt() == 0 -> {
                shares_input.error = "Please input shares count"
            }
            amount_input.text.toString().toBigDecimal() < 0.toBigDecimal() * shares_input.text.toString().toBigDecimal() -> {
                amount_input.error = "Amount must above ${0 * shares_input.text.toString().toDouble()} ETH"
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) {
            return
        }
        if (requestCode == TOKEN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data.getParcelableExtra<WalletToken>("data")?.let {
                if (it.dataId != null && it.dataId != 0) {
                    viewModel.token.value = DbContext.data.select(WalletToken::class).where(WalletToken::dataId eq it.dataId).get().first()
                } else {
                    viewModel.token.value = WalletTokenEntity().apply {
                        this.wallet = wallet_spinner.selectedItem as WalletData
                        this.tokenBalance = (wallet_spinner.selectedItem as WalletData).currentBalance
                        this.token = ERC20TokenEntity().apply {
                            this.symbol = "ETH"
                            this.name = "ETH"
                        }
                    }
                }
            }
        }
    }
}

