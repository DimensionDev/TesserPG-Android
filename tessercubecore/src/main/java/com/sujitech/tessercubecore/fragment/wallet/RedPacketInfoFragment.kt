package com.sujitech.tessercubecore.fragment.wallet

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.createWeb3j
import com.sujitech.tessercubecore.common.extension.dp
import com.sujitech.tessercubecore.common.extension.format
import com.sujitech.tessercubecore.common.extension.toast
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.UserKeyData
import com.sujitech.tessercubecore.data.WalletData
import kotlinx.android.synthetic.main.fragment_red_packet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.util.*

class RedPacketInfoFragment : Fragment(R.layout.fragment_red_packet) {

    var next: (() -> Unit)? = null
    var back: (() -> Unit)? = null

    private val userKeys by lazy {
        DbContext.data.select(UserKeyData::class).get().toList().let {
            ArrayList(it)
        }
    }

    private val wallets by lazy {
        DbContext.data.select(WalletData::class).get().toList()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let { ctx ->
            wallet_spinner.adapter = object : ArrayAdapter<WalletData>(ctx, android.R.layout.simple_spinner_dropdown_item, wallets) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val spinnerView = convertView
                            ?: layoutInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)
                    spinnerView.findViewById<TextView>(android.R.id.text1).updatePadding(0.dp, 0.dp, 0.dp, 0.dp)
                    val item = getItem(position)
                    spinnerView.findViewById<TextView>(android.R.id.text1).text = item?.address
                            ?: ""
                    return spinnerView
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return getView(position, convertView, parent)
                }
            }

            wallet_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    wallets[position].balance?.let {
                        wallet_eth.text = Convert.fromWei(it, Convert.Unit.ETHER).format(4) + " ETH"
                    }

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
                    when {
                        amount_input.text.isNullOrEmpty() || amount_input.text.toString().toBigDecimal() == 0.toBigDecimal() -> {
                            context?.toast("Please input amount")
                        }
                        shares_input.text.isNullOrEmpty() || shares_input.text.toString().toInt() == 0 -> {
                            context?.toast("Please input shares count")
                        }
                        amount_input.text.toString().toBigDecimal() < 0.002.toBigDecimal() * shares_input.text.toString().toBigDecimal() -> {
                            context?.toast("Amount must above ${0.002 * shares_input.text.toString().toDouble()} ETH")
                        }
                        else -> {
                            next?.invoke()
                        }
                    }
                    true
                }

                else -> false
            }
        }
        refreshWalletBalance()
    }


    private fun refreshWalletBalance() {
        wallet_eth_loading.isVisible = true
        wallet_eth.isVisible = false
        val web3j = createWeb3j()
        lifecycleScope.launch(Dispatchers.IO) {
            for (wallet in wallets) {
                val balance = web3j.ethGetBalance(wallet.address, DefaultBlockParameterName.LATEST).send()
                wallet.balance = balance.balance.toBigDecimal()
                withContext(Dispatchers.Main) {
                    DbContext.data.update(wallet).blockingGet()
                }
            }
            withContext(Dispatchers.Main) {
                wallets[wallet_spinner.selectedItemPosition].balance?.let {
                    wallet_eth.text = Convert.fromWei(it, Convert.Unit.ETHER).format(4) + " ETH"
                }
                wallet_eth_loading.isVisible = false
                wallet_eth.isVisible = true
            }
        }
    }
    fun getRedPacketData(): RedPacketData {
        return RedPacketData(
                wallet = wallet_spinner.selectedItem as WalletData,
                amount = amount_input.text.toString().toBigDecimal(),
                shares = shares_input.text.toString().toInt(),
                sender = sender_spinner.selectedItem as UserKeyData
        )
    }
}

data class RedPacketData(
        val wallet: WalletData,
        val amount: BigDecimal,
        val shares: Int,
        val sender: UserKeyData
)