package com.sujitech.tessercubecore.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.sujitech.tessercubecore.R
import kotlinx.android.synthetic.main.keyboard_encrypt.view.*

class EncryptView : KeyboardExtendChildView {
    override val layout: Int
        get() = R.layout.keyboard_encrypt

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    private val builder by lazy {
        PGPEncryptBuilder(context)
    }

    override fun onCreate(args: Any?) {
        super.onCreate(args)
        next_button.setOnClickListener {
            navHost.navigate<ReceiverSelectView>(builder.apply {
                content = encrypt_input.text.toString()
            })
        }
    }

    private val ic by lazy {
        encrypt_input.onCreateInputConnection(EditorInfo())
    }

    override fun getInputConnection(): InputConnection? {
        return ic
    }
}