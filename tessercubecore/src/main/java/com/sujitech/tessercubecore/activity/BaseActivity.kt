package com.sujitech.tessercubecore.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.sujitech.tessercubecore.R


abstract class BaseActivity: AppCompatActivity() {

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        if (toolbar != null) {
           setSupportActionBar(toolbar)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null && item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        onDestroyCallback?.invoke()
    }

    public var onDestroyCallback: (() -> Unit)? = null
}