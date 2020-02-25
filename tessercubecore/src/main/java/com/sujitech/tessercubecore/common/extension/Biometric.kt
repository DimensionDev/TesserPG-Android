package com.sujitech.tessercubecore.common.extension

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun FragmentActivity.biometricAuthentication(
        title: String,
        subTitle: String,
        confirmRequired: Boolean = false
) = suspendCoroutine<Boolean> {
    val biometricManager = BiometricManager.from(this)
    if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS){
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object: BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                it.resume(false)
            }

            override fun onAuthenticationFailed() {
                it.resume(false)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                it.resume(true)
            }
        }
        val biometricPrompt = BiometricPrompt(this, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subTitle)
                .setNegativeButtonText("Cancel")
                // DeviceCredential will trigger onAuthenticationError
//                .setDeviceCredentialAllowed(true)
                .setConfirmationRequired(confirmRequired)
                .build()
        biometricPrompt.authenticate(promptInfo)
    } else {
        it.resume(true)
    }
}