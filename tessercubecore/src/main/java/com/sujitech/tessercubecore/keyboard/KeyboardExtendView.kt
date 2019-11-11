package com.sujitech.tessercubecore.keyboard

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.MessageDataUtils
import com.sujitech.tessercubecore.common.Settings
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.adapter.ChildViewAdapter
import com.sujitech.tessercubecore.common.adapter.updateItemsSource
import com.sujitech.tessercubecore.common.extension.applyMessageData
import com.sujitech.tessercubecore.common.extension.dp
import com.sujitech.tessercubecore.common.extension.getClipboardText
import com.sujitech.tessercubecore.common.toColor
import com.sujitech.tessercubecore.data.*
import kotlinx.android.synthetic.main.view_keyboard_encrypt.view.*
import kotlinx.android.synthetic.main.view_keyboard_interpret.view.*
import kotlinx.android.synthetic.main.widget_keyboard_extend_view.view.*
import moe.tlaster.kotlinpgp.KotlinPGP
import moe.tlaster.kotlinpgp.data.EncryptParameter
import moe.tlaster.kotlinpgp.data.PublicKeyData
import moe.tlaster.kotlinpgp.data.VerifyStatus
import moe.tlaster.kotlinpgp.isPGPMessage
import kotlin.math.max

class KeyboardExtendView : FrameLayout, ToolbarActionsListener {
    override suspend fun requestInterpret() {
        post {
//            TransitionManager.beginDelayedTransition(this)
            encrypt_view_progress_container.isVisible = true
            keyboard_interpret_message_card.isVisible = false
            keyboard_interpret_status_text.text = ""
            keyboard_interpret_container.setBackgroundColor(Color.TRANSPARENT)
        }

        actualInterpret()

        post {
//            TransitionManager.beginDelayedTransition(this)
            encrypt_view_progress_container.isVisible = false
        }
    }

    private suspend fun actualInterpret() {
        val clip = context.getClipboardText()
        if (!clip.isPGPMessage) {
            //nothing to interpret
            post {
                keyboard_interpret_status_text.text = context.getString(R.string.interpret_status_nothing)
            }
            return
        }
        var messageData: MessageData? = null
        var errorMessage = R.string.interpret_status_failed
        try {
            messageData = MessageDataUtils.getMessageDataFromEncryptedContent(context, clip)
        } catch (e: Throwable) {
            errorMessage = R.string.error_interpret_private_key_mismatch
        }
        if (messageData == null) {
            //Error
            post {
                keyboard_interpret_status_text.text = context.getString(errorMessage)
                keyboard_interpret_container.setBackgroundColor(Color.RED)
            }
            return
        }
        post {
            keyboard_interpret_status_text.text = when (messageData.verifyStatus) {
                VerifyStatus.NO_SIGNATURE -> context.getString(R.string.sign_status_no_signature)
                VerifyStatus.SIGNATURE_BAD -> context.getString(R.string.sign_status_bad)
                VerifyStatus.SIGNATURE_OK -> context.getString(R.string.sign_status_ok)
                VerifyStatus.UNKNOWN_PUBLIC_KEY -> context.getString(R.string.sign_status_unknown)
            }
            keyboard_interpret_message_card.isVisible = true
            keyboard_interpret_container.setBackgroundColor(messageData.verifyStatus.toColor())
            keyboard_interpret_message_card.messageData = messageData
        }
    }

    override suspend fun requestEncrypt(content: String, pubKeys: List<ContactData>): String {
        post {
            TransitionManager.beginDelayedTransition(this)
            encrypt_view_progress_container.isVisible = true
        }

        val result = actualEncrypt(content, pubKeys)

        post {
            TransitionManager.beginDelayedTransition(this)
            encrypt_view_progress_container.isVisible = false
        }
        return result
    }

    private suspend fun actualEncrypt(content: String, pubKeys: List<ContactData>): String {
        var signature = Settings.get("keyboard_default_signature", "-2").toIntOrNull() ?: -2
        if (signature == -2) {
            signature = 0
        }
        val userKeyData = DbContext.data.select(UserKeyData::class).get().elementAtOrNull(signature)
        val result = if (userKeyData != null) {
            KotlinPGP.encrypt(EncryptParameter(
                    message = content,
                    publicKey = pubKeys.map { PublicKeyData(it.pubKeyContent) },// + data.filter { it.isUserKey }.map { PublicKeyData(it.pubKeyContent, true) },
                    enableSignature = true,
                    privateKey = userKeyData.priKey,
                    password = if (userKeyData.hasPassword) {
                        UserPasswordStorage.get(context, userKeyData.uuid) ?: ""
                    } else {
                        ""
                    }
            ))
        } else {
            KotlinPGP.encrypt(EncryptParameter(
                    message = content,
                    publicKey = pubKeys.map { PublicKeyData(it.pubKeyContent) } + data.filter { it.isUserKey }.map { PublicKeyData(it.pubKeyContent, true) }
            ))
        }
        MessageDataEntity().also {
            it.applyMessageData(content, result, userKeyData?.contactData, pubKeys)
            post {
                DbContext.data.insert(it).blockingGet()
            }
        }
        return result
    }

    override fun getCurrentPage(): Int {
        return viewPager.currentItem
    }

    override fun toPage(index: Int) {
        viewPager.setCurrentItem(index, false)
    }

    private lateinit var listener: Listener

    private val ic by lazy {
        encrypt_view_input.onCreateInputConnection(EditorInfo())
    }

    interface Listener {
        fun onItemSelected(data: ContactData)
        fun getCurrentItems(): List<ContactData>
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    private val onItemClicked: (Any, AutoAdapter.ItemClickEventArg<ContactData>) -> Unit = { sender, args ->
        listener.onItemSelected(args.item)
        updateContact()
    }

    private var data = listOf<ContactData>()

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_keyboard_extend_view, this)
        viewPager.apply {
            adapter = ChildViewAdapter(viewPager)
            post {
                currentItem = viewPager.childCount - 1
            }
        }
        encrypt_view_list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AutoAdapter<ContactData>(R.layout.item_message_contact, 8.dp).apply {
                bindText(R.id.item_message_contact_title) {
                    it.name
                }
                bindText(R.id.item_message_contact_desc) {
                    "(${it.email})"
                }
                bindText(R.id.item_message_contact_hash) {
                    val fingerPrint = it.keyData.firstOrNull()?.fingerPrint
                    fingerPrint?.substring(max(fingerPrint.length - 8, 0)) ?: ""
                }
                itemClicked += onItemClicked
            }
        }
        encrypt_view_input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchContract(s.toString())
            }
        })
        updateContact()
//        encrypt_view_encrypt_button.setOnClickListener {
//            val text = listener.requireAllText()
//            val base64 = Base64.encodeToString(text.toByteArray(Charset.defaultCharset()), Base64.DEFAULT)
//            listener.overrideAllText(base64)
//        }
//        encrypt_view_decrypt_button.setOnClickListener {
//            val text = listener.requireAllText()
//            val dec = Base64.decode(text, Base64.DEFAULT)?.toString(Charset.defaultCharset())
//            if (dec != null) {
//                listener.overrideAllText(dec)
//            }
//        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.GONE) {
            encrypt_view_input.setText("")
            viewPager.setCurrentItem(viewPager.childCount - 1, false)
        } else {
            updateContact()
        }

    }

    private fun updateContact() {
        data = DbContext.data.select(ContactData::class).get().toList()
        encrypt_view_list.updateItemsSource(data.filter {
            ::listener.isInitialized && !listener.getCurrentItems().contains(it)
        })
    }

    private fun searchContract(name: String) {
        encrypt_view_list.updateItemsSource(data.filter {
            it.name.contains(name) && !listener.getCurrentItems().contains(it)
        })
    }


    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun createInputConnection(): InputConnection? {
        return if (viewPager.currentItem == viewPager.childCount - 1) {
            ic
        } else {
            null
        }
    }

    fun toggle() {
//        val parentView = parent
//        if (parentView is ViewGroup) {
//            TransitionManager.beginDelayedTransition(parentView)
//        }
        visibility = if (visibility == GONE) {
            VISIBLE
        } else {
            GONE
        }
    }
}

//data class ContractData (
//        val name: String,
//        val email: String,
//        val hash: String
//)
