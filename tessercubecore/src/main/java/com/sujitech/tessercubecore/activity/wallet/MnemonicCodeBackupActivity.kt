package com.sujitech.tessercubecore.activity.wallet

import android.os.Bundle
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.adapter.updateItemsSource
import com.sujitech.tessercubecore.common.extension.toActivity
import kotlinx.android.synthetic.main.activity_mnemonic_code_backup.*
import kotlinx.android.synthetic.main.item_me_wallet.*
import org.web3j.crypto.WalletUtils

class MnemonicCodeBackupActivity : BaseActivity() {

    private val password by lazy {
        intent.getStringExtra("password")
    }

    private val mnemonic by lazy {
        intent.getStringExtra("mnemonic")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mnemonic_code_backup)

        recycler_view.adapter = AutoAdapter<String>(R.layout.item_mnemonic_code).apply {
            bindText(android.R.id.text1) {
                it
            }
        }

        recycler_view.updateItemsSource(mnemonic?.split(" "))


        val credentials = WalletUtils.loadBip39Credentials(password, mnemonic)
        item_key_name.text = "Wallet ${credentials.address.take(6)}"
        item_key_fingerprint.text = credentials.address

        confirm_button.setOnClickListener {
            toActivity<MnemonicCodeConfirmActivity>(intent)
            finish()
        }

        cancel_button.setOnClickListener {
            finish()
        }
    }
}
