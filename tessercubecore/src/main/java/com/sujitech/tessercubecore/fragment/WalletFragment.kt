package com.sujitech.tessercubecore.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.wallet.CreateWalletActivity
import com.sujitech.tessercubecore.activity.wallet.ImportWalletActivity
import com.sujitech.tessercubecore.activity.wallet.SendRedpacketActivity
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.adapter.getItemsSource
import com.sujitech.tessercubecore.common.adapter.updateItemsSource
import com.sujitech.tessercubecore.common.createWeb3j
import com.sujitech.tessercubecore.common.extension.format
import com.sujitech.tessercubecore.common.extension.shareText
import com.sujitech.tessercubecore.common.extension.toActivity
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.WalletData
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Convert

class WalletFragment : ViewPagerFragment() {
    private lateinit var addPopupMenu: PopupMenu

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.inflateMenu(R.menu.me_toolbar)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_add -> {
                    if (!::addPopupMenu.isInitialized) {
                        addPopupMenu = PopupMenu(context!!, view.findViewById(R.id.menu_add)).apply {
                            inflate(R.menu.me_add_key)
                            setOnMenuItemClickListener {
                                when (it.itemId) {
                                    R.id.menu_create_key -> {
                                        context.toActivity<CreateWalletActivity>()
                                        true
                                    }
                                    R.id.menu_import_key -> {
                                        context.toActivity<ImportWalletActivity>()
                                        true
                                    }
                                    else -> false
                                }
                            }
                        }
                    }
                    addPopupMenu.show()
                    true
                }
                else -> false
            }
        }
        create_key_button.setOnClickListener {
            context.toActivity<CreateWalletActivity>()
        }
        import_key_button.setOnClickListener {
            context.toActivity<ImportWalletActivity>()
        }
        recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AutoAdapter<WalletData>(R.layout.item_me_wallet).apply {
                bindText(R.id.item_key_name) {
                    "Wallet ${it.address.take(6)}"
                }
                bindText(R.id.item_key_fingerprint) {
                    it.address
                }
                bindText(R.id.item_key_type) {
                    it.balance?.takeIf {
                        it > 0.toBigDecimal()
                    }?.let {
                        Convert.fromWei(it, Convert.Unit.ETHER).format(4) + " ETH"
                    } ?: "0 ETH"
                    //it.address
                }
                itemClicked += { sender, args ->
                    PopupMenu(context, sender as View).apply {
                        this.gravity = Gravity.END
                        inflate(R.menu.me_user_wallet_recycler_view)
                        setOnMenuItemClickListener {
                            when (it.itemId) {
                                R.id.menu_share_address -> {
                                    context.shareText(args.item.address)
                                    true
                                }
                                R.id.menu_delete -> {
                                    deleteWallet(args.item)
                                    true
                                }
                                else -> false
                            }
                        }
                    }.show()
                }
            }
        }
        create_redpacket_button.setOnClickListener {
            context.toActivity<SendRedpacketActivity>()
        }

        subscribeWalletList()
        refresh_layout.setOnRefreshListener {
            refreshWalletBalance()
        }
    }

    private fun refreshWalletBalance() {
        recycler_view.getItemsSource<WalletData>()?.let {
            val web3j = createWeb3j()
            lifecycleScope.launch(Dispatchers.IO) {
                for (wallet in it.toList()) {
                    val balance = web3j.ethGetBalance(wallet.address, DefaultBlockParameterName.LATEST).send()
                    wallet.balance = balance.balance.toBigDecimal()
                    withContext(Dispatchers.Main) {
                        DbContext.data.update(wallet).blockingGet()
                    }
                }
                withContext(Dispatchers.Main) {
                    refresh_layout.isRefreshing = false
                }
            }
        }
    }

    private fun deleteWallet(item: WalletData) {
        DbContext.data.delete(item).blockingGet()
    }


    private lateinit var walletSubscribe: Disposable

    private fun subscribeWalletList() {
        this.walletSubscribe = DbContext.data.select(WalletData::class).get().observableResult().subscribe {
            recycler_view.updateItemsSource(it.toList())
            empty_key_container.isVisible = !it.any()
            create_redpacket_button.isVisible = it.any()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!walletSubscribe.isDisposed) {
            walletSubscribe.dispose()
        }
    }
}