package com.sujitech.tessercubecore.activity

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import androidx.viewpager.widget.ViewPager
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.adapter.ChildViewAdapter
import com.sujitech.tessercubecore.common.extension.toActivity
import kotlinx.android.synthetic.main.activity_tutorial.*

class TutorialActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
        viewPager.adapter = ChildViewAdapter(viewPager)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                TransitionManager.beginDelayedTransition(root)
                next_button.isVisible = position != viewPager.childCount - 1
                confirm_button.isVisible = position == viewPager.childCount - 1
            }
        })
        next_button.setOnClickListener {
            viewPager.currentItem++
        }
        confirm_button.setOnClickListener {
            toActivity<IndexActivity>()
            finish()
        }
        skip_button.setOnClickListener {
            toActivity<IndexActivity>()
            finish()
        }
    }
}
