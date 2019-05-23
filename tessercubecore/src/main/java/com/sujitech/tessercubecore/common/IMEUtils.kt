package com.sujitech.tessercubecore.common

import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager

object IMEUtils {
    fun isThisImeEnabled(context: Context, imm: InputMethodManager): Boolean {
        val packageName = context.packageName
        for (imi in imm.enabledInputMethodList) {
            if (packageName == imi.packageName) {
                return true
            }
        }
        return false
    }

    fun isThisImeCurrent(context: Context, imm: InputMethodManager): Boolean {
        val imi = getInputMethodInfoOf(context.packageName, imm)
        val currentImeId = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return imi != null && imi.id == currentImeId
    }

    private fun getInputMethodInfoOf(packageName: String, imm: InputMethodManager): InputMethodInfo? {
        for (imi in imm.inputMethodList) {
            if (packageName == imi.packageName) {
                return imi
            }
        }
        return null
    }
}
