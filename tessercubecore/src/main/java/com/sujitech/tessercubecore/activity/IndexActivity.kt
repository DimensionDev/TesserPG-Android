package com.sujitech.tessercubecore.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.viewpager.widget.ViewPager
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.FloatingHoverUtils
import com.sujitech.tessercubecore.common.IMEUtils
import com.sujitech.tessercubecore.common.Settings
import com.sujitech.tessercubecore.common.adapter.FragmentAdapter
import com.sujitech.tessercubecore.fragment.ContactsFragment
import com.sujitech.tessercubecore.fragment.MeFragment
import com.sujitech.tessercubecore.fragment.MessagesFragment
import kotlinx.android.synthetic.main.activity_index.*


class IndexActivity : BaseActivity() {

    private val fragments by lazy {
        listOf(
                ContactsFragment(),
                MessagesFragment(),
                MeFragment()
        )
    }

    private val menus by lazy {
        (0 until navigation_menu.menu.size()).map {
            navigation_menu.menu.getItem(it).itemId
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_index)
        FloatingHoverUtils.checkPermission(this)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (!Settings.get("has_ime_entry", false) &&
                (!IMEUtils.isThisImeCurrent(this, imm) || !IMEUtils.isThisImeEnabled(this, imm))) {
            Settings.set("has_ime_entry", true)
            startActivity(Intent(this, Class.forName("com.android.inputmethod.latin.setup.SetupActivity")))
        }

        navigation_menu.setOnNavigationItemSelectedListener {
            val position = menus.indexOf(it.itemId)
            viewPager.setCurrentItem(position, true)
            true
        }
        viewPager.apply {
            adapter = FragmentAdapter(fragments, supportFragmentManager)
            offscreenPageLimit = (adapter as FragmentAdapter).count - 1
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                    if (state == 0) {
                        fragments[viewPager.currentItem].onPageSelected()
                    }
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    navigation_menu.selectedItemId = navigation_menu.menu.getItem(position).itemId
                }

            })
        }

        if (intent?.hasExtra("pager_index") == true) {
            viewPager.currentItem = intent.getIntExtra("pager_index", 0)
        }

        viewPager.post {
            fragments[viewPager.currentItem].onPageSelected()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return fragments[viewPager.currentItem].onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }
}

