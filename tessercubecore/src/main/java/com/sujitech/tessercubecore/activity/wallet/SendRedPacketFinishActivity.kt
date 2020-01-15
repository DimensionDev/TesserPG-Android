package com.sujitech.tessercubecore.activity.wallet

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.RedPacketData
import io.requery.kotlin.eq
import kotlinx.android.synthetic.main.activity_send_red_packet_finish.*

class SendRedPacketFinishActivity : BaseActivity() {
    private val data by lazy {
        DbContext.data.select(RedPacketData::class).where(RedPacketData::dataId eq intent.getParcelableExtra<RedPacketData>("data").dataId).get().firstOrNull()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_red_packet_finish)
        red_packet.data = data
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.common_done, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                R.id.menu_done -> {
                    finish()
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
