package com.sujitech.tessercubecore.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.ObservableCollection
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.adapter.getItemsSource
import com.sujitech.tessercubecore.common.extension.toast
import com.sujitech.tessercubecore.data.ContactData
import kotlinx.android.synthetic.main.widget_keyboard_encrypt_toolbar.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


interface ToolbarActionsListener {
    fun getCurrentPage(): Int
    fun toPage(index: Int)
    suspend fun requestInterpret()
    suspend fun requestEncrypt(content: String, pubKeys: List<ContactData>): String
}

class KeyboardEncryptToolBar : RelativeLayout, KeyboardExtendView.Listener {
    override fun getCurrentItems(): List<ContactData> {
        return encrypt_toolbar_list.getItemsSource() ?: emptyList()
    }

    override fun onItemSelected(data: ContactData) {
        encrypt_toolbar_list.getItemsSource<ContactData>()?.add(data)
    }

    private lateinit var listener: Listener
    private lateinit var toolbarActionsListener: ToolbarActionsListener

    interface Listener {
        fun getSelection(): String
        fun overrideSelection(text: String)
        fun requireAllText(): String
        fun overrideAllText(text: String)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    private val onListItemsChanged: (Any, ObservableCollection.CollectionChangedEventArg) -> Unit = { sender, args ->
        val itemCount = encrypt_toolbar_list.getItemsSource<ContactData>()?.size
        if (itemCount != null && itemCount > 0) {
            if (!encrypt_toolbar_list.isVisible) {
                TransitionManager.beginDelayedTransition(action_container)
                encrypt_toolbar_list.isVisible = true
                encrypt_toolbar_interpret.hideText()
                encrypt_toolbar_encrypt.hideText()
            }
        } else {
            if (encrypt_toolbar_list.isVisible) {
                TransitionManager.beginDelayedTransition(action_container)
                encrypt_toolbar_list.isVisible = false
                encrypt_toolbar_interpret.showText()
                encrypt_toolbar_encrypt.showText()
            }
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_keyboard_encrypt_toolbar, this)
        encrypt_toolbar_interpret.setOnClickListener {
            if (toolbarActionsListener.getCurrentPage() != 0) {
                toolbarActionsListener.toPage(0)
            }
            GlobalScope.launch {
                toolbarActionsListener.requestInterpret()
            }
        }
        encrypt_toolbar_encrypt.setOnClickListener {
            if (toolbarActionsListener.getCurrentPage() != 1) {
                toolbarActionsListener.toPage(1)
            } else {
                kotlin.runCatching {
                    val selection = listener.getSelection()
                    val text = if (selection.isEmpty()) {
                        listener.requireAllText()
                    } else {
                        selection
                    }
                    val contacts = encrypt_toolbar_list.getItemsSource<ContactData>()
                    if (contacts != null && contacts.any()) {
                        val pubKeys = contacts.toList()//.map { it.pubKeyContent }

                        clearSelection()
                        GlobalScope.launch {
                            val result = toolbarActionsListener.requestEncrypt(text, pubKeys)
                            if (selection.isEmpty()) {
                                listener.overrideAllText(result)
                            } else {
                                listener.overrideSelection(result)
                            }
                        }
                    } else {
                        context.toast(context.getString(R.string.error_compose_no_receiver))
                    }
//                    val dec = Base64.decode(text, Base64.DEFAULT)?.toString(Charset.defaultCharset())
//                    if (dec != null) {
//                        listener.overrideAllText(dec)
//                    }
                }.onFailure {
                    //TODO:
                }
            }
        }
        encrypt_toolbar_list.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = AutoAdapter<ContactData>(R.layout.item_keyboard_toolbar_contract).apply {
                bindText(android.R.id.text1) {
                    it.name
                }
                items.collectionChanged += this@KeyboardEncryptToolBar.onListItemsChanged
            }
        }
    }


    fun setToolbarActionsListener(listener: ToolbarActionsListener) {
        this.toolbarActionsListener = listener
    }

    fun setListener(listener: Listener) {
        this.listener = listener
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

    fun onClose() {
        clearSelection()
    }

    fun clearSelection() {
        encrypt_toolbar_list.getItemsSource<ContactData>()?.clear()
    }
}