package com.sujitech.tessercubecore.common

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle


object UserPasswordStorage {
    fun save(context: Context, id: String, password: String, userData: Bundle? = null): Boolean {
        val accountManager = AccountManager.get(context)
        val account = Account(id, context.packageName)
        return accountManager.addAccountExplicitly(account, password, userData)
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