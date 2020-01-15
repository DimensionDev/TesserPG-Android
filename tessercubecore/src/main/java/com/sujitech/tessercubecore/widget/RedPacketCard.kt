package com.sujitech.tessercubecore.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.prettyTime
import com.sujitech.tessercubecore.data.*
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

        red_packet_sender.text = "From: ${value.senderName}"

        value.blockCreationTime?.let {
            Date(it * 1000)
        }?.let {
            // TODO: do not format older than 7 days
            red_packet_time.text = prettyTime.format(it)
        }

        red_packet_shares.text = "${value.actualValue} ${value.unit} in total / ${value.passwords.count()} shares"
        red_packet_message.text = value.sendMessage

        red_packet_state2.text = when (value.status) {
            RedPacketStatus.initial -> {
                "Ready to send"
            }
            RedPacketStatus.pending -> {
                "Sending..."
            }
            RedPacketStatus.fail -> {
                "Fail to send"
            }
            RedPacketStatus.normal -> {
                "Sent ${value.actualValue} ${value.unit}"
            }
            RedPacketStatus.incoming -> {
                "Incoming Red Packet"
            }
            RedPacketStatus.claimPending -> {
                "Claiming"
            }
            RedPacketStatus.claimed -> {
                value.claimAmount?.let {
                    "Got ${it.formatToken(value.erC20Token != null, value.erC20Token?.decimals)} ${value.unit}"
                } ?: "Got 0 ${value.unit}" // TODO
            }
            RedPacketStatus.expired -> {
                "Red Packet expired"
            }
            RedPacketStatus.empty -> {
                "Too late to get any"
            }
            RedPacketStatus.refundPending -> {
                "Refunding..."
            }
            RedPacketStatus.refunded -> {
                value.refundAmount?.let {
                    "Refunded ${it.formatToken(value.erC20Token != null, value.erC20Token?.decimals)} ${value.unit}"
                } ?: "Refunded 0 ${value.unit}" // TODO
            }
        }
    }

}