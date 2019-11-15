package com.sujitech.tessercubecore.activity.wallet

import android.os.Bundle
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.extension.toActivity
import kotlinx.android.synthetic.main.activity_create_wallet.*


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
        if (password.isEmpty()) {
            return
        }
//        val web3 = Web3j.build(HttpService("SERVER"))
//
//        val clientVersion = web3.web3ClientVersion().sendAsync().get()
//        if (!clientVersion.hasError()) {
//            //Connected
//        } else {
//            //Show Error
//        }
//        val result = WalletUtils.generateBip39Wallet(password, cacheDir)
//        WalletUtils.loadBip39Credentials()
//        val receipt = Transfer.sendFunds(web3, credentials, "ContractID", BigDecimal(1), Convert.Unit.ETHER).sendAsync().get()


        toActivity<MnemonicCodeBackupActivity>()
    }
}
