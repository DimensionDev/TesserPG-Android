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
            red_packet_state.text = "Sent ${redPacketData.price} ETH"
        } else {
            if (redPacketData.state == RedPacketState.claimed) {
                red_packet_state.text = "Claimed ${Convert.fromWei(redPacketData.price, Convert.Unit.ETHER)} ETH"
            } else {
                red_packet_state.text = redPacketData.state?.name ?: ""
            }
        }
        red_packet_shares.text = "${redPacketData.shares?.toString()} Shares"
    }
}