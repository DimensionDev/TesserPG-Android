package com.sujitech.tessercubecore.activity

import android.os.Bundle
import com.sujitech.tessercubecore.common.Settings
import com.sujitech.tessercubecore.common.extension.toActivity

class SplashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Settings.get("is_first_run",  true)) {
            Settings.set("is_first_run", false)
            toActivity<TutorialActivity>()
        } else {
            toActivity<IndexActivity>()
        }
        finish()
    }
}
