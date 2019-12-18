package com.sujitech.tessercubecore.activity.wallet

import android.content.Intent
import android.os.Bundle
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.extension.toActivity
import com.sujitech.tessercubecore.common.extension.toast
import kotlinx.android.synthetic.main.activity_create_wallet.*
import org.web3j.crypto.WalletUtils
import java.io.File

class CreateWalletActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_wallet)
        confirm_button.setOnClickListener {
            createWallet()
        }
    }

    private fun createWallet() {
        val password = password_input.text.toString()
        val passwordConfirm = password_confirm_input.text.toString()
        if (password.isEmpty() || password.length < 8 || password != passwordConfirm) {
            // TODO
            toast(getString(R.string.error_password_mismatch))
            return
        }

        val result = WalletUtils.generateBip39Wallet(password, cacheDir)

        File(cacheDir, result.filename).delete()

        toActivity<MnemonicCodeBackupActivity>(Intent().apply {
            putExtra("password", password)
            putExtra("mnemonic", result.mnemonic)
        })

        finish()

    }
}
