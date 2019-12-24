package com.sujitech.tessercubecore.activity.wallet

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.extension.getClipboardText
import com.sujitech.tessercubecore.common.extension.toActivity
import com.sujitech.tessercubecore.common.wallet.RedPacketPayloadHelper
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.RedPacketStatus
import kotlinx.android.synthetic.main.activity_open_red_packet.*

class OpenRedPacketActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_red_packet)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.compose_toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                R.id.menu_done -> {
                    openRedPacket()
                    return true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onResume() {
        super.onResume()
        val clip = getClipboardText()
        if (RedPacketPayloadHelper.isRedPacketPayload(clip)) {
            interpret_content.setText(clip)
        }
    }

    private fun openRedPacket() {
        val content = interpret_content.text.toString()
        if (!RedPacketPayloadHelper.isRedPacketPayload(content)) {
            // TODO: notification
            return
        }
        val unpacked = RedPacketPayloadHelper.unpack(content)
        val data = RedPacketPayloadHelper.parse(unpacked)
        data.status = RedPacketStatus.incoming
        DbContext.data.insert(data).blockingGet().let {
            toActivity<IncomingRedPacketActivity>(Intent().apply {
                putExtra("data", it)
            })
            finish()
        }
    }
}
