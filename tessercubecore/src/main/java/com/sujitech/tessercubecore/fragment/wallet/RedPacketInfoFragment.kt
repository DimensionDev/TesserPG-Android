package com.sujitech.tessercubecore.fragment.wallet

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.extension.dp
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.UserKeyData
import com.sujitech.tessercubecore.data.WalletData
import kotlinx.android.synthetic.main.fragment_red_packet.*
import java.math.BigDecimal
import java.util.*

class RedPacketInfoFragment : Fragment(R.layout.fragment_red_packet) {

    var next: (() -> Unit)? = null
    var back: (() -> Unit)? = null

    private val userKeys by lazy {
        DbContext.data.select(UserKeyData::class).get().toList().let {
            ArrayList(it).apply {
                add(0, null)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let { ctx ->
            val wallets = DbContext.data.select(WalletData::class).get().toList()
            wallet_spinner.adapter = object : ArrayAdapter<WalletData>(ctx, android.R.layout.simple_spinner_dropdown_item, wallets) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val spinnerView = convertView
                            ?: layoutInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)
                    val item = getItem(position)
                    spinnerView.findViewById<TextView>(android.R.id.text1).text = item?.address
                            ?: ""
                    return spinnerView
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return getView(position, convertView, parent)
                }
            }

            sender_spinner.adapter = object : ArrayAdapter<UserKeyData>(ctx, R.layout.item_message_contact, userKeys) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = convertView
                            ?: layoutInflater.inflate(R.layout.item_message_contact, parent, false)
                    view.setPadding(8.dp, 8.dp, 8.dp, 8.dp)
                    val item = getItem(position)
                    val contactData = item?.contactData
                    if (contactData != null) {
                        view.findViewById<TextView>(R.id.item_message_contact_title).text = contactData.name
                        view.findViewById<TextView>(R.id.item_message_contact_desc).text = contactData.email
                        view.findViewById<TextView>(R.id.item_message_contact_hash).text = contactData.keyData.firstOrNull()?.fingerPrint?.toUpperCase()?.takeLast(8)
                                ?: ""
                    } else {
                        view.findViewById<TextView>(R.id.item_message_contact_title).text = getString(R.string.compose_without_signature)
                    }
                    return view
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return getView(position, convertView, parent)
                }
            }
        }
        toolbar.setNavigationOnClickListener {
            back?.invoke()
        }
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                android.R.id.home -> {
                    back?.invoke()
                    true
                }

                R.id.menu_next -> {
                    next?.invoke()
                    true
                }

                else -> false
            }
        }
    }

    fun getRedPacketData(): RedPacketData {
        return RedPacketData(
                wallet = wallet_spinner.selectedItem as WalletData,
                amount = amount_input.text.toString().toBigDecimal(),
                shares = shares_input.text.toString().toLong(),
                sender = sender_spinner.selectedItem as UserKeyData
        )
    }
}

data class RedPacketData(
        val wallet: WalletData,
        val amount: BigDecimal,
        val shares: Long,
        val sender: UserKeyData
)