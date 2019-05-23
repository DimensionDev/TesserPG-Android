package com.sujitech.tessercubecore.activity.contact

import android.content.Intent
import android.os.Bundle
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.extension.getClipboardText
import com.sujitech.tessercubecore.common.extension.toActivity
import com.sujitech.tessercubecore.common.extension.toContactData
import com.sujitech.tessercubecore.common.extension.toast
import com.sujitech.tessercubecore.common.isAppCenterEnabled
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.KeyData
import io.requery.kotlin.eq
import kotlinx.android.synthetic.main.activity_import_contact.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.tlaster.kotlinpgp.KotlinPGP
import moe.tlaster.kotlinpgp.isPGPPublicKey

class ImportContactActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_contact)
        import_button.setOnClickListener {
            importContact()
        }

        if (intent?.action == Intent.ACTION_SEND) {
            if ("text/plain" == intent.type) {
                val content = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (content.isNullOrEmpty()) {
                    toast(getString(R.string.error_import_empty))
                    finish()
                } else if (!content.isPGPPublicKey) {
                    toast(getString(R.string.error_public_key_format))
                    finish()
                } else {
                    import_text.setText(content)
                    importContact()
                }
            }
        }

    }

    private fun importContact() {
        val publicKeyText = import_text.text.toString()
        if (publicKeyText.isNotEmpty() && publicKeyText.isNotBlank()) {
            GlobalScope.launch {
                kotlin.runCatching {
                    val pgpPublicKeyRing = KotlinPGP.getPublicKeyRingFromString(publicKeyText)
                    if (DbContext.data.select(KeyData::class).where(KeyData::keyId eq pgpPublicKeyRing.publicKey.keyID).get().any()) {
                        runOnUiThread {
                            toast(getString(R.string.error_import_contact_already_exist))
                        }
                    } else {
                        val contact = pgpPublicKeyRing.toContactData(publicKeyText)
                        runOnUiThread {
                            val result = DbContext.data.insert(contact).blockingGet()
                            toActivity<ContactDetailActivity>(Intent().apply {
                                putExtra("data", result)
                            })
                            finish()
                        }
                    }
                }.onFailure {
                    runOnUiThread {
                        import_text.error = getString(R.string.error_import_contact)
    //                            Toast.makeText(this@ImportContactActivity, "Import error", Toast.LENGTH_SHORT).show()
                    }
                    if (isAppCenterEnabled) {
                        Analytics.trackEvent("Import Public Key Error", EventProperties().apply {
                            set("PublicKey", publicKeyText)
                            set("StackTrace", it.stackTrace.joinToString(System.lineSeparator()) { it.toString() })
                            set("Message", it.message)
                        })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val clip = getClipboardText()
        if (clip.isPGPPublicKey) {
            import_text.setText(clip)
        }
    }
}
