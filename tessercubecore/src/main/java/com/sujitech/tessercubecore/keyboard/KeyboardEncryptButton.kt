package com.sujitech.tessercubecore.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.extension.getClipboardText
import kotlinx.android.synthetic.main.widget_keyboard_encrypt_button.view.*
import moe.tlaster.kotlinpgp.isPGPMessage

class KeyboardEncryptButton : FrameLayout, ClipboardManager.OnPrimaryClipChangedListener {
    override fun onPrimaryClipChanged() {
        val clip = context.getClipboardText()
        button_dot.isVisible = clip.isPGPMessage
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs, defStyle)
    }

    private fun init(context: Context, attrs: AttributeSet, defStyle: Int) {
        LayoutInflater.from(context).inflate(R.layout.widget_keyboard_encrypt_button, this)
        button_dot.isVisible = false
//        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//        clipboardManager.addPrimaryClipChangedListener(this)
    }

    fun hideDot() {
        button_dot.isVisible = false
    }
}