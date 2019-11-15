package com.sujitech.tessercubecore.activity.wallet

import android.os.Bundle
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.extension.toActivity
import kotlinx.android.synthetic.main.activity_create_wallet.*

class MnemonicCodeBackupActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mnemonic_code_backup)
        confirm_button.setOnClickListener {
            toActivity<MnemonicCodeConfirmActivity>()
        }
    }
}
