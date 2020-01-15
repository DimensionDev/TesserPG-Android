package com.sujitech.tessercubecore.activity.wallet

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.observe
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.extension.task
import com.sujitech.tessercubecore.data.WalletData
import com.sujitech.tessercubecore.viewmodel.wallet.AddCustomTokenViewModel
import kotlinx.android.synthetic.main.activity_add_custom_token.*

class AddCustomTokenActivity : BaseActivity() {

    private val data by lazy {
        intent.getParcelableExtra<WalletData>("data")
    }

    private val viewModel by viewModels<AddCustomTokenViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_custom_token)
        viewModel.success.observe(this) {
            commit_button.isEnabled = it
        }
        viewModel.name.observe(this) {
            token_name.setText(it)
        }
        viewModel.decimals.observe(this) {
            token_decimals.setText(it.toString())
        }
        viewModel.symbol.observe(this) {
            token_symbol.setText(it)
        }
        token_address.doAfterTextChanged {
            viewModel.getTokenInfo(data, it)
        }
        commit_button.setOnClickListener {
            task {
                viewModel.addToken(data, token_address.text.toString())
                finish()
            }
        }
    }

}
