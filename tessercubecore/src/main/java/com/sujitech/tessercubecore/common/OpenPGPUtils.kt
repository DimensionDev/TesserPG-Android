package com.sujitech.tessercubecore.common

import android.graphics.Color
import moe.tlaster.kotlinpgp.data.VerifyStatus

fun VerifyStatus.toColor(): Int {
    return when (this) {
        VerifyStatus.NO_SIGNATURE -> Color.TRANSPARENT
        VerifyStatus.SIGNATURE_BAD -> Color.RED
        VerifyStatus.SIGNATURE_OK -> Color.GREEN
        VerifyStatus.UNKNOWN_PUBLIC_KEY -> Color.YELLOW
    }
}