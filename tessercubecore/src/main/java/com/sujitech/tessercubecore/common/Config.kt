package com.sujitech.tessercubecore.common

import android.os.Build
import com.sujitech.tessercubecore.BuildConfig
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.appContext

val isAppCenterEnabled by lazy {
    if (BuildConfig.DEBUG) {
        appContext.getString(R.string.app_center_id).isNullOrEmpty() && appContext.resources.getBoolean(R.bool.is_app_center_enabled)
    } else {
        appContext.resources.getBoolean(R.bool.is_app_center_enabled)
    }

}