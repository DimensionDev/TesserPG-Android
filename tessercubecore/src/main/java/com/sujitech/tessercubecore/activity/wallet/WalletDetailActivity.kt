package com.sujitech.tessercubecore.activity.wallet

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.extension.formatWei
import com.sujitech.tessercubecore.common.extension.shareText
import com.sujitech.tessercubecore.common.extension.toActivity
import com.sujitech.tessercubecore.data.WalletData
import com.sujitech.tessercubecore.data.WalletToken
import com.sujitech.tessercubecore.data.formatToken
import com.sujitech.tessercubecore.viewmodel.wallet.WalletDetailViewModel
import kotlinx.android.synthetic.main.activity_wallet_detail.*

class WalletDetailActivity : BaseActivity() {

    private val viewModel by viewModels<WalletDetailViewModel>()

    private val data by lazy {
        intent.getParcelableExtra<WalletData>("data")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_detail)
        copy_address_button.setOnClickListener {
            shareText(data.address)
        }
        add_token_button.setOnClickListener {
            toActivity<AddTokenActivity>(Intent().putExtra("data", data))
        }
        recycler_view.apply {
            layoutManager = LinearLayoutManager(this@WalletDetailActivity)
            adapter = AutoAdapter<WalletToken>(R.layout.item_token).apply {
                items = viewModel.tokens
                bindText(R.id.token_name) {
                    it.token.name
                }
                bindText(R.id.token_symbol) {
                    it.token.symbol
                }
                bindText(R.id.token_value) {
                    it.tokenBalance?.formatToken(true, it.token.decimals).toString()
                }
                bindImage(R.id.token_image) {
                    "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/ethereum/assets/${it.token.address}/logo.png"
                }
            }
        }
        viewModel.loadToken(data)
        viewModel.wallet.observe(this) {
            item_key_name.text = "Wallet ${it.address.take(6)}"
            item_key_fingerprint.text = it.address
            item_key_type.text = it.balance?.takeIf {
                it > 0.toBigDecimal()
            }?.formatWei() ?: "0 ETH"
        }
    }
}
