package com.sujitech.tessercubecore.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout

abstract class KeyboardExtendChildView : FrameLayout, IKeyboardExtendViewChild {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override lateinit var navHost: IKeyboardNavHost
    override lateinit var extendViewHost: IKeyboardExtendViewHost
    abstract val layout: Int
    protected var arguments: Any? = null

    override fun getInputConnection(): InputConnection? {
        return null
    }

    override fun onCreate(args: Any?) {
        arguments = args
        LayoutInflater.from(context).inflate(layout, this)
    }

    override fun onRemove() {

    }

    fun finish() {
        navHost.finish(this)
    }
}