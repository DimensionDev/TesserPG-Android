package com.sujitech.tessercubecore.activity.keypair

import android.os.Bundle
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.extension.toActivity
import kotlinx.android.synthetic.main.activity_import_key.*

class ImportKeyActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_key)
        cancel_button.setOnClickListener {
            finish()
        }
        paste_private_key_button.setOnClickListener {
            toActivity<InputPrivateKeyActivity>()
            finish()
        }
        scan_qr_code_button.setOnClickListener {

        }
        mnemonic_words_button.setOnClickListener {

        }
    }
}
