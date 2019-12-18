package com.sujitech.tessercubecore.fragment

import android.view.KeyEvent
import androidx.fragment.app.Fragment

abstract class ViewPagerFragment: Fragment {
    constructor() : super()
    constructor(contentLayoutId: Int) : super(contentLayoutId)


    open fun onPageSelected() {

    }

    open fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }
}