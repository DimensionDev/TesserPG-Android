package com.sujitech.tessercubecore.activity.keypair

import android.content.Intent
import android.os.Bundle
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.activity.IndexActivity
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.extension.*
import com.sujitech.tessercubecore.data.DbContext
import kotlinx.android.synthetic.main.activity_input_private_key.*
import moe.tlaster.kotlinpgp.KotlinPGP
import moe.tlaster.kotlinpgp.isPGPPrivateKey

class InputPrivateKeyActivity : BaseActivity() {
    var needToIndex = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_private_key)
        import_button.setOnClickListener {
            val privateKeyStr = private_key_input.text.toString()
            val password = password_input.text.toString()
            if (privateKeyStr.isEmpty()) {
                private_key_input.error = getString(R.string.error_private_key_empty)
            } else {
                task {
                    kotlin.runCatching {
                        val privateKey = KotlinPGP.getSecretKeyRingFromString(privateKeyStr, password)
                        val data = privateKey.toUserKeyData(privateKeyStr, password)
                        if (data.hasPassword) {
                            UserPasswordStorage.save(this@InputPrivateKeyActivity, data.uuid, password)
                        }
                        runOnUiThread {
                            DbContext.data.insert(data).blockingGet()
                            if (needToIndex) {
                                toActivity<IndexActivity>(Intent().apply {
                                    putExtra("pager_index", 2)
                                })
                            }
                            finish()
                        }
                    }.onFailure {
                        runOnUiThread {
                            private_key_input.error = getString(R.string.error_import_private_key)
                        }
                    }
                }
            }
        }

        if (intent?.action == Intent.ACTION_SEND) {
            if ("text/plain" == intent.type) {
                val content = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (content.isNullOrEmpty()) {
                    toast(getString(R.string.error_import_empty))
                    finish()
                } else if (!content.isPGPPrivateKey) {
                    toast(getString(R.string.error_private_key_format))
                    finish()
                } else {
                    needToIndex = true
                    private_key_input.setText(content)
                    password_input.requestFocus()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val clipText = getClipboardText()
        if (clipText.isPGPPrivateKey) {
            private_key_input.setText(clipText)
        }
    }
}
