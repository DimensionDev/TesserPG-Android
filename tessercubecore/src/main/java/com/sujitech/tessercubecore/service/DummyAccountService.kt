package com.sujitech.tessercubecore.service


import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder

class DummyAccountService : Service() {
    private inner class Authenticator : AbstractAccountAuthenticator(this@DummyAccountService) {

        override fun editProperties(response: AccountAuthenticatorResponse, accountType: String): Bundle? {
            return null
        }

        override fun addAccount(response: AccountAuthenticatorResponse, accountType: String, authTokenType: String,
                                requiredFeatures: Array<String>, options: Bundle): Bundle? {
            response.onResult(Bundle())
            return null
        }

        override fun confirmCredentials(response: AccountAuthenticatorResponse, account: Account, options: Bundle): Bundle? {
            return null
        }

        override fun getAuthToken(response: AccountAuthenticatorResponse, account: Account, authTokenType: String,
                                  options: Bundle): Bundle? {
            return null
        }

        override fun getAuthTokenLabel(authTokenType: String): String? {
            return null
        }

        override fun updateCredentials(response: AccountAuthenticatorResponse, account: Account, authTokenType: String,
                                       options: Bundle): Bundle? {
            return null
        }

        override fun hasFeatures(response: AccountAuthenticatorResponse, account: Account, features: Array<String>): Bundle? {
            return null
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return Authenticator().iBinder
    }
}