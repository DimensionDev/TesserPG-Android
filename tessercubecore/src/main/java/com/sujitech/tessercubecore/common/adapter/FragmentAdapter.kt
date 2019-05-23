package com.sujitech.tessercubecore.common.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter


class FragmentAdapter(val list: List<Fragment>, fm: FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getItem(position: Int): Fragment = list[position]
    override fun getCount(): Int = list.size
}

