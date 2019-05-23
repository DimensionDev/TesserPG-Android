package com.sujitech.tessercubecore.common

import com.microsoft.appcenter.analytics.Analytics

object DataTracking {
    fun track(name: String, data: Map<String, String>) {
        Analytics.trackEvent(name, data)
    }
}