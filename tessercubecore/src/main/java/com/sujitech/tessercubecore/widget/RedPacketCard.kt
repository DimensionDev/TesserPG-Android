package com.sujitech.tessercubecore.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.cardview.widget.CardView
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.RedPacketUtils
import com.sujitech.tessercubecore.common.extension.dp
import com.sujitech.tessercubecore.common.prettyTime
import com.sujitech.tessercubecore.data.MessageData
import com.sujitech.tessercubecore.data.RedPacketState
import kotlinx.android.synthetic.main.widget_red_packet_card.view.*
import org.web3j.utils.Convert

class RedPacketCard : CardView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_red_packet_card, this)
        cardElevation = 2.dp.toFloat()
        radius = 8.dp.toFloat()
    }

    var data: MessageData? = null
        set(value) {
            field = value
            onDataChanged(value)
        }

    private fun onDataChanged(value: MessageData?) {
        val redPacketData = value?.redPacketData ?: return
        val data = RedPacketUtils.parse(value.content)
        red_packet_sender.text = data.senderId
        red_packet_time.text = context.getString(R.string.message_composed_time, prettyTime.format(value.composeTime))
        if (redPacketData.fromMe) {
            if (redPacketData.state == RedPacketState.claimed) {
                red_packet_state.text = "Got ${Convert.fromWei(redPacketData.price, Convert.Unit.ETHER)} ETH"
                red_packet_shares.text = "${if ((redPacketData.remainPrice ?: 0.toBigDecimal()) > 0.toBigDecimal()) {
                    "${Convert.fromWei(redPacketData.remainPrice, Convert.Unit.ETHER)} ETH remains / "
                } else {
                    ""
                } }${redPacketData.shares?.toString()} Shares"
            } else {
                red_packet_state.text = "Sending ${redPacketData.price} ETH"
                red_packet_shares.text = "Giving ${redPacketData.price} ETH / ${redPacketData.shares?.toString()} Shares"
            }
            if (redPacketData.collectedCount != null && redPacketData.collectedCount!! > 0) {
                red_packet_state2.text = "${redPacketData.collectedCount} / ${redPacketData.shares} collected"
            } else {
                red_packet_state2.text = "Ready for collection"
            }
        } else {
            when (redPacketData.state) {
                RedPacketState.unknown -> {

                }
                RedPacketState.notClaimed -> {
                    red_packet_state.text = "Incoming Red Packet"
                    red_packet_shares.text = "${if ((redPacketData.remainPrice ?: 0.toBigDecimal()) > 0.toBigDecimal()) {
                        "${Convert.fromWei(redPacketData.remainPrice, Convert.Unit.ETHER)} ETH remains / "
                    } else {
                        ""
                    } }${redPacketData.shares?.toString()} Shares"
                }
                RedPacketState.claimed -> {
                    red_packet_state.text = "Got ${Convert.fromWei(redPacketData.price, Convert.Unit.ETHER)} ETH"
                    red_packet_shares.text = "${if ((redPacketData.remainPrice ?: 0.toBigDecimal()) > 0.toBigDecimal()) {
                        "${Convert.fromWei(redPacketData.remainPrice, Convert.Unit.ETHER)} ETH remains / "
                    } else {
                        ""
                    } }${redPacketData.shares?.toString()} Shares"
                }
                RedPacketState.claimFailed -> {
                    red_packet_state.text = "Incoming Red Packet"
                    red_packet_shares.text = "Claim failed, tap to retry"
                }
                RedPacketState.claimLate -> {
                    red_packet_state.text = "Too late to get any"
                    red_packet_shares.text = "${if ((redPacketData.remainPrice ?: 0.toBigDecimal()) > 0.toBigDecimal()) {
                        "${Convert.fromWei(redPacketData.remainPrice, Convert.Unit.ETHER)} ETH remains / "
                    } else {
                        ""
                    } }${redPacketData.shares?.toString()} Shares"
                }
            }

            red_packet_state2.text = context.getString(R.string.message_interpreted_time, prettyTime.format(value.interpretTime))
        }
    }
}