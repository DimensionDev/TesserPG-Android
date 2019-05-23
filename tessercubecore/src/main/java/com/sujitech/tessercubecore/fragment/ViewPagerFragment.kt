package com.sujitech.tessercubecore.fragment

import android.view.KeyEvent
import androidx.fragment.app.Fragment

abstract class ViewPagerFragment: Fragment() {
    open fun onPageSelected() {

    }

    open fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }
}