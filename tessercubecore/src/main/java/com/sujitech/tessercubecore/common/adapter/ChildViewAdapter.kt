package com.sujitech.tessercubecore.common.adapter

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager

class ChildViewAdapter(private val viewPager: ViewPager) : PagerAdapter() {
    init {
        viewPager.offscreenPageLimit = viewPager.childCount
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return viewPager.getChildAt(position)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as View
    }

    override fun getCount(): Int {
        return viewPager.childCount
    }

}