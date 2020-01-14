package com.sujitech.tessercubecore.activity.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.data.WalletData
import com.sujitech.tessercubecore.data.WalletToken
import com.sujitech.tessercubecore.data.formatToken
import com.sujitech.tessercubecore.viewmodel.wallet.TokenSelectViewModel
import kotlinx.android.synthetic.main.activity_token_select.*

class TokenSelectActivity : AppCompatActivity() {

    private val viewModel by viewModels<TokenSelectViewModel>()

    private val data by lazy {
        intent.getParcelableExtra<WalletData>("data")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_token)
        viewModel.init(data)
        search_bar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.filter(newText)
                return false
            }
        })
        recycler_view.apply {
            layoutManager = LinearLayoutManager(this@TokenSelectActivity)
            adapter = AutoAdapter<WalletToken>(R.layout.item_token).apply {
                items = viewModel.actualTokens
                bindText(R.id.token_name) {
                    it.token.name
                }
                bindText(R.id.token_symbol) {
                    it.token.symbol
                }
                bindText(R.id.token_value) { token ->
                    token.tokenBalance?.let {
                        if (token.token.symbol != "ETH") {
                            "${it.formatToken(true, token.token.decimals)} ${token.token.symbol}"
                        } else {
                            "$it ${token.token.symbol}"
                        }
                    } ?: "0 ${token.token.symbol}"
                }
                bindImage(R.id.token_image) {
                    if (!it.token.address.isNullOrEmpty()) {
                        "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/ethereum/assets/${it.token.address}/logo.png"
                    } else {
                        ""
                    }
                }
                itemClicked.observe(this@TokenSelectActivity) {
                    setResult(Activity.RESULT_OK, Intent().putExtra("data", it.item))
                    finish()
                }
            }
        }
    }
}
