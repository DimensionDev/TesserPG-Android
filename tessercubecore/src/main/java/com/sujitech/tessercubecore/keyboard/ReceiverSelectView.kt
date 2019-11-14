package com.sujitech.tessercubecore.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import androidx.core.widget.doOnTextChanged
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.adapter.getItemsSource
import com.sujitech.tessercubecore.common.adapter.updateItemsSource
import com.sujitech.tessercubecore.common.extension.dp
import com.sujitech.tessercubecore.data.ContactData
import com.sujitech.tessercubecore.data.DbContext
import kotlinx.android.synthetic.main.keyboard_receiver_select.view.*
import kotlin.math.max

class ReceiverSelectView : KeyboardExtendChildView, View.OnFocusChangeListener {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override val layout: Int
        get() = R.layout.keyboard_receiver_select

    private val onItemClicked: (Any, AutoAdapter.ItemClickEventArg<ContactData>) -> Unit = { sender, args ->
        encrypt_toolbar_list.getItemsSource<ContactData>()?.add(args.item)
        search_input.setText("")
        updateContact()
    }
    private var data = listOf<ContactData>()
    private val builder by lazy {
        arguments
    }

    override fun onCreate(args: Any?) {
        super.onCreate(args)
        back_button.setOnClickListener {
            navHost.goBack()
        }
        commit_button.setOnClickListener {
            builder?.let {
                it as? IKeyboardInputWithContact
            }?.also {
                it.contacts.addAll(encrypt_toolbar_list.getItemsSource() ?: emptyList())
            }?.let {
                it as? IKeyboardInputBuilder
            }?.let {
                val result = it.build()
                setResultToInputConnection(result)
            }
        }
        search_input.onFocusChangeListener = this
        extendViewHost.hideKeyboard()
        encrypt_contact_list.apply {
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
        encrypt_toolbar_list.apply {
            adapter = AutoAdapter<ContactData>(R.layout.item_keyboard_toolbar_contract).apply {
                bindText(android.R.id.text1) {
                    it.name
                }
            }
        }
        search_input.doOnTextChanged { text, _, _, _ ->
            searchContract(text.toString())
        }
        updateContact()
    }

    private fun setResultToInputConnection(result: String) {
        val ic = extendViewHost.actualInputConnection()
        val extractedText = ic.getExtractedText(ExtractedTextRequest(), 0)
        val startIndex = extractedText.startOffset + extractedText.selectionStart
        val endIndex = extractedText.startOffset + extractedText.selectionEnd
        ic.setComposingRegion(startIndex, endIndex)
        ic.setComposingText(result, 1)
        ic.finishComposingText()
        extendViewHost.hideExtend()
    }

    private fun searchContract(name: String) {
        encrypt_contact_list.updateItemsSource(data.filter {
            it.name.contains(name) && encrypt_toolbar_list.getItemsSource<ContactData>()?.contains(it) != true
        })
    }

    private fun updateContact() {
        data = DbContext.data.select(ContactData::class).get().toList()
        encrypt_contact_list.updateItemsSource(data.filter {
            encrypt_toolbar_list.getItemsSource<ContactData>()?.contains(it) != true
        })
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (hasFocus) {
            extendViewHost.showKeyboard()
        } else {
            extendViewHost.hideKeyboard()
        }
    }

    override fun onRemove() {
        super.onRemove()
        search_input.onFocusChangeListener = null
        extendViewHost.showKeyboard()
    }


    private val ic by lazy {
        search_input.onCreateInputConnection(EditorInfo())
    }

    override fun getInputConnection(): InputConnection? {
        return ic
    }
}