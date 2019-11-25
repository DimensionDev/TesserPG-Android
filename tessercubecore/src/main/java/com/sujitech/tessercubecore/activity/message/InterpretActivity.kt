package com.sujitech.tessercubecore.activity.message

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.sujitech.tessercubecore.App
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.activity.IndexActivity
import com.sujitech.tessercubecore.common.MessageDataUtils
import com.sujitech.tessercubecore.common.PrivateKeyNotFoundError
import com.sujitech.tessercubecore.common.extension.getClipboardText
import com.sujitech.tessercubecore.common.extension.task
import com.sujitech.tessercubecore.common.extension.toActivity
import com.sujitech.tessercubecore.common.extension.toast
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.MessageData
import com.sujitech.tessercubecore.widget.MessageCard
import io.requery.kotlin.eq
import kotlinx.android.synthetic.main.activity_interpret.*
import moe.tlaster.kotlinpgp.isPGPMessage

class InterpretActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interpret)
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    val content = intent.getStringExtra(Intent.EXTRA_TEXT)
                    processContent(content) {
                        toActivity<IndexActivity>(Intent().apply {
                            putExtra("pager_index", 1)
                        })
                    }
                }
            }
            Intent.ACTION_PROCESS_TEXT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && intent.hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
                    val content = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT).toString()
                    val readonly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)
                    processContent(content, allowDuplicate = readonly) { messageData ->
                        if (!readonly) {
                            val intent = Intent().apply {
                                putExtra(Intent.EXTRA_PROCESS_TEXT, messageData.content)
                            }
                            setResult(Activity.RESULT_OK, intent)
                        } else {
                            App.floatingController.floatingView.findViewById<MessageCard>(R.id.floating_decrypt_message_card).also {
                                it.visibility = View.VISIBLE
                                it.messageData = messageData
                            }
                            App.floatingController.floatingView.findViewById<View>(R.id.floating_contact_container).visibility = View.GONE
                            App.floatingController.showContentView()
                        }
                    }
                }
            }
        }
    }

    private fun processContent(content: String?, allowDuplicate: Boolean = false, callback: (MessageData) -> Unit) {
        if (content.isNullOrEmpty()) {
            toast(getString(R.string.error_import_empty))
            finish()
        } else if (!content.isPGPMessage) {
            toast(getString(R.string.error_pgp_message_format))
            finish()
        } else {
            interpret_content.setText(content)
            interpret(allowDuplicate, callback)
        }
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                R.id.menu_done -> {
                    interpret()
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun interpret(allowDuplicate: Boolean = false, callback: ((MessageData) -> Unit)? = null) {
        val pgpContent = interpret_content.text.toString()
        if (pgpContent.isEmpty()) {
            toast(getString(R.string.error_interpret_empty_content))
            return
        }
        if (!allowDuplicate && DbContext.data.select(MessageData::class).where(MessageData::rawContent eq pgpContent).get().any()) {
            toast(getString(R.string.error_interpret_already_exist))
            return
        }
        task {
            try {
                val messageData = MessageDataUtils.getMessageDataFromEncryptedContent(this@InterpretActivity, pgpContent)
                if (messageData != null) {
                    runOnUiThread {
                        DbContext.data.insert(messageData).blockingGet()
                        callback?.invoke(messageData)
                        finish()
                    }
                } else {
                    runOnUiThread {
                        toast(getString(R.string.error_interpret_private_key_mismatch))
                    }
                }
            } catch (e: PrivateKeyNotFoundError) {
                runOnUiThread {
                    toast(getString(R.string.error_interpret_private_key_mismatch))
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.compose_toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }
    override fun onResume() {
        super.onResume()
        val clip = getClipboardText()
        if (clip.isPGPMessage) {
            interpret_content.setText(clip)
        }
    }
}
