package com.sujitech.tessercubecore.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.prettyTime
import com.sujitech.tessercubecore.data.*
import com.sujitech.tessercubecore.data.RedPacketStatus.*
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

        red_packet_shares.text = "${value.actualValue} ${value.unit} in total / ${value.passwords.count()} shares"

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
                "Sent ${value.actualValue} ${value.unit}"
            }
            incoming -> {
                "Incoming Red Packet"
            }
            claimPending -> {
                "Claiming"
            }
            claimed -> {
                value.claimAmount?.let {
                    "Got ${it.formatToken(value.erC20Token != null, value.erC20Token?.decimals)} ${value.unit}"
                } ?: "Got 0 ${value.unit}" // TODO
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
                    "Refunded ${it.formatToken(value.erC20Token != null, value.erC20Token?.decimals)} ${value.unit}"
                } ?: "Refunded 0 ${value.unit}" // TODO
            }
        }
    }

}