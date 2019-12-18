package com.sujitech.tessercubecore.fragment.wallet

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.wallet.CreateWalletActivity
import com.sujitech.tessercubecore.activity.wallet.ImportWalletActivity
import com.sujitech.tessercubecore.activity.wallet.SendRedpacketActivity
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.extension.formatWei
import com.sujitech.tessercubecore.common.extension.shareText
import com.sujitech.tessercubecore.common.extension.toActivity
import com.sujitech.tessercubecore.data.RedPacketData
import com.sujitech.tessercubecore.data.WalletData
import com.sujitech.tessercubecore.fragment.ViewPagerFragment
import com.sujitech.tessercubecore.viewmodel.wallet.WalletViewModel
import kotlinx.android.synthetic.main.fragment_wallet.*
import me.relex.circleindicator.CircleIndicator3

class WalletFragment : ViewPagerFragment(R.layout.fragment_wallet) {
    private lateinit var addPopupMenu: PopupMenu
    private val viewModel by viewModels<WalletViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            //            context.toActivity<ImportWalletActivity>()
        }
        recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AutoAdapter<RedPacketData>(R.layout.item_red_packet).apply {
                items = viewModel.redPacket
                withHeader(R.layout.header_wallet)
                bindHeader { header ->
                    header.findViewById<ViewPager2>(R.id.view_pager).also { viewPager2 ->
                        viewPager2.adapter = AutoAdapter<WalletData>(R.layout.item_me_wallet).apply {
                            items = viewModel.wallets
                            bindText(R.id.item_key_name) {
                                "Wallet ${it.address.take(6)}"
                            }
                            bindText(R.id.item_key_fingerprint) {
                                it.address
                            }
                            bindText(R.id.item_key_type) {
                                it.balance?.takeIf {
                                    it > 0.toBigDecimal()
                                }?.formatWei() ?: "0 ETH"
                            }
                            itemLongPressed.observe(viewLifecycleOwner, Observer { args ->
                                PopupMenu(args.view.context, args.view).apply {
                                    this.gravity = Gravity.END
                                    inflate(R.menu.me_user_wallet_recycler_view)
                                    setOnMenuItemClickListener {
                                        when (it.itemId) {
                                            R.id.menu_share_address -> {
                                                args.view.context.shareText(args.item.address)
                                                true
                                            }
                                            R.id.menu_delete -> {
//                                deleteWallet(it.item)
                                                true
                                            }
                                            else -> false
                                        }
                                    }
                                }.show()
                            })

                            registerAdapterDataObserver(header.findViewById<CircleIndicator3>(R.id.view_pager_indicator).adapterDataObserver)
                        }
                        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                            override fun onPageSelected(position: Int) {
                                viewModel.currentWallet = viewModel.wallets.elementAtOrNull(position)
                            }
                        })
                        header.findViewById<CircleIndicator3>(R.id.view_pager_indicator).setViewPager(viewPager2)
                    }
                }
            }
        }
        create_redpacket_button.setOnClickListener {
            context.toActivity<SendRedpacketActivity>()
        }

        viewModel.wallets.collectionChanged.observe(viewLifecycleOwner, Observer {
            empty_key_container.isVisible = !viewModel.wallets.any()
        })

        refresh_layout.setOnRefreshListener {
            refresh_layout.isRefreshing = false
        }
    }
}