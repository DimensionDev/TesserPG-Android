package com.sujitech.tessercubecore.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.FloatingHoverUtils
import com.sujitech.tessercubecore.common.IMEUtils
import com.sujitech.tessercubecore.common.Settings
import kotlinx.android.synthetic.main.activity_index.*


class IndexActivity : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_index)
        val navController = findNavController(R.id.nav_host_fragment)
        bottom_navigation_view?.setupWithNavController(navController)
        FloatingHoverUtils.checkPermission(this)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (!Settings.get("has_ime_entry", false) &&
                (!IMEUtils.isThisImeCurrent(this, imm) || !IMEUtils.isThisImeEnabled(this, imm))) {
            Settings.set("has_ime_entry", true)
            startActivity(Intent(this, Class.forName("com.android.inputmethod.latin.setup.SetupActivity")))
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
        }
    }
}

