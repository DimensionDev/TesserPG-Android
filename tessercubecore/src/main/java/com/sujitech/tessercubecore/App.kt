package com.sujitech.tessercubecore

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import com.sujitech.tessercubecore.activity.contact.ContactDetailActivity
import com.sujitech.tessercubecore.common.FloatingHoverUtils
import com.sujitech.tessercubecore.common.MessageDataUtils
import com.sujitech.tessercubecore.common.extension.getClipboardText
import com.sujitech.tessercubecore.common.extension.toContactData
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.KeyData
import com.sujitech.tessercubecore.widget.ContactView
import com.sujitech.tessercubecore.widget.MessageCard
import io.requery.kotlin.eq
import moe.tlaster.floatinghover.FloatingController
import moe.tlaster.kotlinpgp.KotlinPGP
import moe.tlaster.kotlinpgp.isPGPMessage
import moe.tlaster.kotlinpgp.isPGPPublicKey
import org.bouncycastle.bcpg.ArmoredOutputStream

lateinit var appContext: Context
class App : Application(), ClipboardManager.OnPrimaryClipChangedListener {

    companion object {
        val floatingController: FloatingController by lazy {
            FloatingController(appContext, R.layout.floating_decrypt).apply {
                floatingView.also { floating ->
                    floating.findViewById<Button>(R.id.floating_contact_confirm_button).setOnClickListener {
                        val contact = floating.findViewById<ContactView>(R.id.floating_contact_view).contact
                        if (contact != null) {
                            floating.post {
                                val result = DbContext.data.insert(contact).blockingGet()
                                it.context.startActivity(Intent(it.context, ContactDetailActivity::class.java).apply {
                                    putExtra("data", result)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        }
                        floatingController.hide()
                    }
                    floating.findViewById<Button>(R.id.floating_contact_cancel_button).setOnClickListener {
                        floatingController.hide()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(this)
        KotlinPGP.header += "Comment" to "Encrypted with https://tessercube.com/"
        KotlinPGP.header += ArmoredOutputStream.VERSION_HDR to null
    }

    override fun onPrimaryClipChanged() {
        if (!FloatingHoverUtils.hasPermission(appContext)) {
            return
        }
        val text = getClipboardText().trim()
        if (text.isPGPMessage) {
            kotlin.runCatching {
                val messageData = MessageDataUtils.getMessageDataFromEncryptedContent(this, text)
                if (messageData != null) {
                    floatingController.floatingView.findViewById<MessageCard>(R.id.floating_decrypt_message_card).also {
                        it.visibility = View.VISIBLE
                        it.messageData = messageData
                    }
                    floatingController.floatingView.findViewById<View>(R.id.floating_contact_container).visibility = View.GONE
                    floatingController.show()
                }
            }.onFailure {
                // Do nothing
                it.printStackTrace()
            }
        } else if (text.isPGPPublicKey) {
            val pgpPublicKeyRing = KotlinPGP.getPublicKeyRingFromString(text)
            if (!DbContext.data.select(KeyData::class).where(KeyData::keyId eq pgpPublicKeyRing.publicKey.keyID).get().any()) {
                val contact = pgpPublicKeyRing.toContactData(text)
                floatingController.floatingView.findViewById<View>(R.id.floating_contact_container).also {
                    it.visibility = View.VISIBLE
                    it.findViewById<ContactView>(R.id.floating_contact_view).contact = contact
                }
                floatingController.floatingView.findViewById<MessageCard>(R.id.floating_decrypt_message_card).visibility = View.GONE
                floatingController.show()
            }
        }
    }
}
