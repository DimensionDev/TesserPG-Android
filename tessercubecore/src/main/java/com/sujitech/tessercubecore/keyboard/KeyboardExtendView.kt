package com.sujitech.tessercubecore.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.transition.TransitionManager
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.extension.dp
import kotlinx.android.synthetic.main.widget_keyboard_extend_view.view.*
import java.util.*

class KeyboardExtendView : FrameLayout, IKeyboardExtendViewHost, IKeyboardNavHost {
    private val backStack: Stack<View> = Stack()

    interface Listener {
        fun getActualInputConnection(): InputConnection
        fun finishInput()
    }

    private lateinit var listener: Listener

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_keyboard_extend_view, this)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.GONE) {
            clearNavigation()
            showKeyboard()
        } else {
            initialNavigate()
        }

    }

    private fun initialNavigate() {
        navigate<EncryptView>()
    }

    private fun clearNavigation() {
        encrypt_child_container.removeAllViews()
        backStack.clear()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun createInputConnection(): InputConnection? {
        return encrypt_child_container.getChildAt(0)?.let {
            it as? IKeyboardExtendViewChild
        }?.getInputConnection()
    }

    private val keyboardView by lazy {
        parent?.let {
            it as? ViewGroup
        }?.findViewById<View>(context.resources.getIdentifier("keyboard_view", "id", context.packageName))
    }

    override fun showKeyboard() {
        keyboardView?.isVisible = true
        encrypt_child_container.updateLayoutParams {
            height = 200.dp
        }
    }

    override fun hideKeyboard() {
        keyboardView?.isVisible = false
        encrypt_child_container.updateLayoutParams {
            height = 200.dp + (keyboardView?.height ?: 0)
        }
    }

    override fun <T : View> navigate(clazz: Class<T>, args: Any?) {
        listener.finishInput()
        encrypt_child_container.getChildAt(0)?.let {
            backStack.push(it)
        }
        TransitionManager.beginDelayedTransition(encrypt_child_container)
        encrypt_child_container.removeAllViews()
        val view = clazz.getConstructor(Context::class.java).newInstance(context)
        if (view is IKeyboardExtendViewChild) {
            view.extendViewHost = this
            view.navHost = this
            view.onCreate(args)
        }
        encrypt_child_container.addView(view)
    }

    override fun goBack() {
        listener.finishInput()
        encrypt_child_container.getChildAt(0)?.let {
            it as? IKeyboardExtendViewChild
        }?.onRemove()
        TransitionManager.beginDelayedTransition(encrypt_child_container)
        encrypt_child_container.removeAllViews()
        if (backStack.any()) {
            encrypt_child_container.addView(backStack.pop())
        }
    }

    override fun finish(view: View) {
        if (encrypt_child_container.getChildAt(0) == view) {
            goBack()
        } else {
            backStack.remove(view)
        }
    }

    override fun actualInputConnection(): InputConnection {
        return listener.getActualInputConnection()
    }

    override fun hideExtend() {
        isVisible = false
    }

    fun toggle() {
        isVisible = !isVisible
    }

    override fun onChanged(args: AutoAdapter.ItemClickEventArg<ContactData>) {
        listener?.onItemSelected(args.item)
        updateContact()
    }
}