package com.sujitech.tessercubecore.activity.wallet

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.extension.shareText
import com.sujitech.tessercubecore.common.extension.task
import com.sujitech.tessercubecore.common.extension.toActivity
import com.sujitech.tessercubecore.common.wallet.RedPacketPayloadHelper
import com.sujitech.tessercubecore.data.*
import com.sujitech.tessercubecore.viewmodel.wallet.RedPacketClaimerData
import com.sujitech.tessercubecore.viewmodel.wallet.RedPacketDetailViewModel
import io.requery.kotlin.eq
import kotlinx.android.synthetic.main.activity_red_packet_detail.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RedPacketDetailActivity : BaseActivity() {

    private val viewModel by viewModels<RedPacketDetailViewModel>()

    private val data by lazy {
        DbContext.data.select(RedPacketData::class).where(RedPacketData::dataId eq intent.getParcelableExtra<RedPacketData>("data").dataId).get().firstOrNull()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_red_packet_detail)
        red_packet.data = data
        detail_sender.text = data?.senderName
        detail_message.text = data?.sendMessage
        recycler_view.apply {
            layoutManager = LinearLayoutManager(this@RedPacketDetailActivity)
            adapter = AutoAdapter<RedPacketClaimerData>(R.layout.item_red_packet_claimer).apply {
                this.items = viewModel.claimers
                bindText(R.id.claimer_name) {
                    it.name
                }
                bindText(R.id.claimer_address) {
                    it.address.take(6)
                }
                bindText(R.id.claimer_amount) {
                    "${it.amount.formatToken(data.erC20Token != null, data?.erC20Token?.decimals)} ${data.unit}"
                }
            }
        }
        updateActionByStatus()
        viewModel.loadClaimerList(data)
    }

    private fun updateActionByStatus() {
        data?.let {
            when (it.status) {
                RedPacketStatus.initial -> {
                    // TODO
                }
                RedPacketStatus.pending -> {
                    detail_left_action.isVisible = true
                    detail_right_action.isVisible = true
                    detail_left_action.isEnabled = false
                    detail_right_action.isEnabled = false
                    detail_left_action.text = "Open"
                    detail_right_action.text = "Share"
                }
                RedPacketStatus.fail -> {
                    // TODO
                }
                RedPacketStatus.normal -> {
                    detail_left_action.isVisible = true
                    detail_right_action.isVisible = true
                    detail_left_action.text = "Open"
                    detail_right_action.text = "Share"
                    detail_left_action.setOnClickListener {
                        openRedPacket()
                    }
                    detail_right_action.setOnClickListener {
                        sharePayload()
                    }
                }
                RedPacketStatus.incoming -> {
                    detail_full_action.isVisible = true
                    detail_full_action.text = "Open"
                    detail_full_action.setOnClickListener {
                        openRedPacket()
                    }
                }
                RedPacketStatus.claimPending -> {
                    if (data.createNonce == null) {
                        detail_full_action.isVisible = true
                        detail_full_action.isEnabled = false
                        detail_full_action.text = "Trying to Claim..."
                    } else {
                        detail_left_action.isVisible = true
                        detail_right_action.isVisible = true
                        detail_left_action.isEnabled = false
                        detail_left_action.text = "Opening..."
                        detail_right_action.text = "Share"
                        detail_right_action.setOnClickListener {
                            sharePayload()
                        }
                    }
                }
                RedPacketStatus.claimed -> {
                    if (data.createNonce == null) {
                        detail_full_action.isVisible = true
                        detail_full_action.isEnabled = false
                        detail_full_action.text = "Already Opened"
                    } else {
                        detail_left_action.isVisible = true
                        detail_right_action.isVisible = true
                        detail_left_action.isEnabled = false
                        detail_left_action.text = "Already Opened"
                        detail_right_action.text = "Share"
                        detail_right_action.setOnClickListener {
                            sharePayload()
                        }
                    }
                }
                RedPacketStatus.expired -> {
                    if (data.createNonce == null) {
                        detail_full_action.isVisible = true
                        detail_full_action.isEnabled = false
                        detail_full_action.text = "Red Packet expired"
                    } else {
                        detail_full_action.isVisible = true
                        detail_full_action.text = "Get Refunded"
                        detail_full_action.setOnClickListener {
                            refund()
                        }
                    }
                }
                RedPacketStatus.empty -> {
                    if (data.createNonce == null) {
                        detail_full_action.isVisible = true
                        detail_full_action.isEnabled = false
                        detail_full_action.text = "Too late to get any"
                    } else {
                        detail_left_action.isVisible = true
                        detail_right_action.isVisible = true
                        detail_left_action.isEnabled = false
                        detail_left_action.isEnabled = false
                        detail_left_action.text = "All claimed"
                        detail_right_action.text = "Share"
                    }
                }
                RedPacketStatus.refundPending -> {
                    detail_full_action.isVisible = true
                    detail_full_action.isEnabled = false
                    detail_full_action.text = "Refunding..."
                }
                RedPacketStatus.refunded -> {
                    detail_full_action.isVisible = true
                    detail_full_action.isEnabled = false
                    detail_full_action.text = "Refunded"
                }
            }
        }
    }

    private fun refund() {
        task {
            viewModel.refund(data)
            withContext(Dispatchers.Main) {
                updateActionByStatus()
            }
        }
    }

    private fun sharePayload() {
        shareText(RedPacketPayloadHelper.pack(data.encPayload!!))
    }

    private fun openRedPacket() {
        toActivity<IncomingRedPacketActivity>(Intent().putExtra("data", data))
        finish()//TODO: refresh ui when navigating back instead of finishing activity
    }
}
