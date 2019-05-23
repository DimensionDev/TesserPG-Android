package com.sujitech.tessercubecore.common

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context


object UserPasswordStorage {
    fun save(context: Context, id: String, password: String): Boolean {
        val accountManager = AccountManager.get(context)
        val account = Account(id, context.packageName)
        return accountManager.addAccountExplicitly(account, password, null)
    }

    fun get(context: Context, id: String): String? {
        val accountManager = AccountManager.get(context)
        val account = accountManager.accounts.firstOrNull {
            it.name == id
        }
        return if (account != null) {
            accountManager.getPassword(account)
        } else {
            null
        }
    }
}