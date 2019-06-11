package com.sujitech.tessercubecore.common

import com.sujitech.tessercubecore.R
import android.app.Application
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes

object DataTracking {
    fun init(app: Application) {
        AppCenter.start(app, app.getString(R.string.app_center_id),
                Analytics::class.java, Crashes::class.java)
    }
    fun track(name: String, data: Map<String, String>) {
        Analytics.trackEvent(name, data)
    }
}