package com.sujitech.tessercubecore.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.view.isVisible
import com.sujitech.tessercubecore.R
import kotlinx.android.synthetic.main.keyboard_red_packet.view.*
import kotlinx.android.synthetic.main.layout_keyboard_mode_selection.view.*

class RedPacketView : KeyboardExtendChildView, View.OnFocusChangeListener {
    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        when (v) {
            shares_input -> {
                currentIc = if (hasFocus) {
                    sharesIc
                } else {
                    null
                }
            }
            amount_input -> {
                currentIc = if (hasFocus) {
                    amountIc
                } else {
                    null
                }
            }
        }
        extendViewHost.finishInput()
    }

    override val layout: Int
        get() = R.layout.keyboard_red_packet

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onCreate(args: Any?) {
        super.onCreate(args)
        mode_button.setOnClickListener {
            mode_selection_container.isVisible = !mode_selection_container.isVisible
        }
        mode_encrypt_button.setOnClickListener {
            navHost.navigate<EncryptView>()
        }
        extendViewHost.hideKeyboard()
        amount_input.onFocusChangeListener = this
        shares_input.onFocusChangeListener = this
    }

    override fun onRemove() {
        super.onRemove()
        amount_input.onFocusChangeListener = null
        shares_input.onFocusChangeListener = null
    }

    private val sharesIc by lazy {
        shares_input.onCreateInputConnection(EditorInfo())
    }

    private val amountIc by lazy {
        amount_input.onCreateInputConnection(EditorInfo())
    }

    private var currentIc: InputConnection? = null
        set(value) {
            field = value
            if (value == null) {
                extendViewHost.hideKeyboard()
            } else {
                extendViewHost.showKeyboard()
            }
        }

    override fun getInputConnection(): InputConnection? {
        return currentIc
    }
}