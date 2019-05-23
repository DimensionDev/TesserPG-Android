package com.sujitech.tessercubecore.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import com.sujitech.tessercubecore.R
import kotlinx.android.synthetic.main.widget_command_button.view.*

class CommandButton : RelativeLayout {
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs, defStyle)
    }

    private fun init(context: Context, attrs: AttributeSet, defStyle: Int) {
        LayoutInflater.from(context).inflate(R.layout.widget_command_button, this)
        context.obtainStyledAttributes(
                attrs, R.styleable.CommandButton, defStyle, 0).apply {
            button_image.setImageDrawable(getDrawable(R.styleable.CommandButton_icon))
            button_text.text = getText(R.styleable.CommandButton_text)
            recycle()
        }
    }

    fun toggle() {
        button_text.isVisible = !button_text.isVisible
    }

    fun showText() {
        button_text.isVisible = true
    }

    fun hideText() {
        button_text.isVisible = false
    }

    var text: CharSequence
        get() = button_text.text
        set(value) {
            button_text.text = value
        }

    var icon: Drawable
        get() = button_image.drawable
        set(value) {
            button_image.setImageDrawable(value)
        }

}