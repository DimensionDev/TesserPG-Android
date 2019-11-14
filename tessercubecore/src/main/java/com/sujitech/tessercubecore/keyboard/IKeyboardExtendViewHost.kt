package com.sujitech.tessercubecore.keyboard

import android.content.Context
import android.view.View
import android.view.inputmethod.InputConnection
import com.sujitech.tessercubecore.common.Settings
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.extension.applyMessageData
import com.sujitech.tessercubecore.data.ContactData
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.MessageDataEntity
import com.sujitech.tessercubecore.data.UserKeyData
import io.requery.kotlin.eq
import moe.tlaster.kotlinpgp.KotlinPGP
import moe.tlaster.kotlinpgp.data.EncryptParameter
import moe.tlaster.kotlinpgp.data.PublicKeyData

interface IKeyboardExtendViewHost {
    fun showKeyboard()
    fun hideKeyboard()
    fun actualInputConnection(): InputConnection
    fun hideExtend()
}

interface IKeyboardExtendViewChild {
    var navHost: IKeyboardNavHost
    var extendViewHost: IKeyboardExtendViewHost
    fun getInputConnection(): InputConnection?
    fun onCreate(args: Any?)
    fun onRemove()
}

interface IKeyboardNavHost {
    fun <T : View> navigate(clazz: Class<T>, args: Any? = null)
    fun goBack()
    fun finish(view: View)
}

inline fun <reified T : View> IKeyboardNavHost.navigate(args: Any? = null) {
    navigate(T::class.java, args)
}

interface IKeyboardInputBuilder {
    val context: Context
    fun build(): String
}

interface IKeyboardInputWithContact {
    val contacts: ArrayList<ContactData>
}

class PGPEncryptBuilder(
        override val context: Context
) : IKeyboardInputBuilder, IKeyboardInputWithContact {
    override fun build(): String {
        return actualEncrypt()
    }

    override val contacts: ArrayList<ContactData> = arrayListOf()
    var content = ""

    private fun actualEncrypt(): String {
        var signature = Settings.get("keyboard_default_signature", "-2").toIntOrNull() ?: -2
        if (signature == -2) {
            signature = 0
        }
        val userKeyData = DbContext.data.select(UserKeyData::class).get().elementAtOrNull(signature)
        val result = if (userKeyData != null) {
            KotlinPGP.encrypt(EncryptParameter(
                    message = content,
                    publicKey = contacts.map { PublicKeyData(it.pubKeyContent) },// + data.filter { it.isUserKey }.map { PublicKeyData(it.pubKeyContent, true) },
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
                    publicKey = contacts.map { PublicKeyData(it.pubKeyContent) } + DbContext.data.select(ContactData::class).where(ContactData::isUserKey eq true).get().map { PublicKeyData(it.pubKeyContent, true) }
            ))
        }
        MessageDataEntity().also {
            it.applyMessageData(content, result, userKeyData?.contactData, contacts)
            DbContext.data.insert(it).blockingGet()
        }
        return result
    }
}