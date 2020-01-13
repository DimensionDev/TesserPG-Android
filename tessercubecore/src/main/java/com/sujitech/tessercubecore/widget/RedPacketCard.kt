package com.sujitech.tessercubecore.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.extension.formatWei
import com.sujitech.tessercubecore.common.prettyTime
import com.sujitech.tessercubecore.data.RedPacketData
import com.sujitech.tessercubecore.data.RedPacketStatus.*
import com.sujitech.tessercubecore.data.passwords
import kotlinx.android.synthetic.main.widget_red_packet_card.view.*
import java.util.*

class RedPacketCard : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_red_packet_card, this)
    }

    var data: RedPacketData? = null
        set(value) {
            field = value
            onDataChanged(value)
        }

    private fun onDataChanged(value: RedPacketData?) {
        if (value == null) {
            return
        }

        red_packet_sender.text = "${value.senderName}(${value.senderAddress})"

        value.blockCreationTime?.let {
            Date(it * 1000)
        }?.let {
            // TODO: do not format older than 1 hour
            red_packet_time.text = prettyTime.format(it)
        }

        red_packet_shares.text = "${value.sendTotal.formatWei()} in total / ${value.passwords.count()} shares"

        red_packet_state.text = when (value.status) {
            initial -> {
                "Ready to send"
            }
            pending -> {
                "Sending..."
            }
            fail -> {
                "Fail to send"
            }
            normal -> {
                "Sent ${value.sendTotal.formatWei()}"
            }
            incoming -> {
                "Incoming Red Packet"
            }
            claimPending -> {
                "Claiming"
            }
            claimed -> {
                value.claimAmount?.let {
                    "Got ${it.formatWei()}"
                } ?: "Got 0 ETH" // TODO
            }
            expired -> {
                "Red Packet expired"
            }
            empty -> {
                "Too late to get any"
            }
            refundPending -> {
                "Refunding..."
            }
            refunded -> {
                value.refundAmount?.let {
                    "Refunded ${it.formatWei()}"
                } ?: "Refunded 0 ETH" // TODO
            }
        }
    }

}