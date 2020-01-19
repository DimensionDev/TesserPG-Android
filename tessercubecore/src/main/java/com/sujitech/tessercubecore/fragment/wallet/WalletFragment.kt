package com.sujitech.tessercubecore.fragment.wallet

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.wallet.*
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.extension.formatWei
import com.sujitech.tessercubecore.common.extension.shareText
import com.sujitech.tessercubecore.common.extension.toActivity
import com.sujitech.tessercubecore.common.wallet.RedPacketPayloadHelper
import com.sujitech.tessercubecore.common.wallet.currentEthNetworkLiveData
import com.sujitech.tessercubecore.common.wallet.currentEthNetworkType
import com.sujitech.tessercubecore.data.*
import com.sujitech.tessercubecore.fragment.ViewPagerFragment
import com.sujitech.tessercubecore.viewmodel.wallet.WalletViewModel
import com.sujitech.tessercubecore.widget.RedPacketCard
import kotlinx.android.synthetic.main.fragment_wallet.*
import me.relex.circleindicator.CircleIndicator3

class WalletFragment : ViewPagerFragment(R.layout.fragment_wallet) {
    private lateinit var networkMenu: PopupMenu
    private lateinit var addPopupMenu: PopupMenu
    private val viewModel by activityViewModels<WalletViewModel>()
    private val walletAdapter by lazy {
        AutoAdapter<WalletData>(R.layout.item_me_wallet).apply {
            items = viewModel.wallets
            bindText(R.id.item_key_name) {
                "Wallet ${it.address.take(6)}"
            }
            bindText(R.id.item_key_fingerprint) {
                it.address
            }
            bindText(R.id.item_key_type) {
                it.currentBalance?.takeIf {
                    it > 0.toBigDecimal()
                }?.formatWei() ?: "0 ETH"
            }
            itemClicked.observe(viewLifecycleOwner, Observer {
                context.toActivity<WalletDetailActivity>(Intent().putExtra("data", it.item))
            })
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
                                deleteWallet(args.item)
                                true
                            }
                            else -> false
                        }
                    }
                }.show()
            })

        }
    }
    private val headerView by lazy {
        LayoutInflater.from(context).inflate(R.layout.header_wallet, recycler_view, false).also { header ->
            header.findViewById<ViewPager2>(R.id.view_pager).also { viewPager2 ->
                walletAdapter.registerAdapterDataObserver(header.findViewById<CircleIndicator3>(R.id.view_pager_indicator).adapterDataObserver)
                viewPager2.adapter = walletAdapter
                viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                        if (positionOffset == 0F) {
                            recycler_view.post {
                                viewModel.currentWallet = viewModel.wallets.elementAtOrNull(position)
                            }
                        }
                    }
                })
                header.findViewById<CircleIndicator3>(R.id.view_pager_indicator).setViewPager(viewPager2)
            }
        }
    }

    private fun deleteWallet(item: WalletData) {
        DbContext.data.delete(item).blockingGet()
    }

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
                R.id.menu_network -> {
                    if (!::networkMenu.isInitialized) {
                        networkMenu = PopupMenu(context!!, view.findViewById(R.id.menu_network)).apply {
                            inflate(R.menu.wallet_network_selection)
                            setOnMenuItemClickListener {
                                when (it.itemId) {
                                    R.id.menu_network_rinkeby -> currentEthNetworkType = RedPacketNetwork.Rinkeby
                                    R.id.menu_network_mainnet -> currentEthNetworkType = RedPacketNetwork.Mainnet
                                    R.id.menu_network_ropsten -> currentEthNetworkType = RedPacketNetwork.Ropsten
                                }
                                true
                            }
                        }
                    }
                    networkMenu.show()
                    true
                }
                else -> false
            }
        }
        currentEthNetworkLiveData.observe(viewLifecycleOwner) {
            toolbar.menu.findItem(R.id.menu_network).title = it.name
            walletAdapter.notifyDataSetChanged()
            if (::networkMenu.isInitialized) {
                when (it) {
                    RedPacketNetwork.Mainnet -> networkMenu.menu.findItem(R.id.menu_network_mainnet).isChecked = true
                    RedPacketNetwork.Rinkeby -> networkMenu.menu.findItem(R.id.menu_network_rinkeby).isChecked = true
                    RedPacketNetwork.Ropsten -> networkMenu.menu.findItem(R.id.menu_network_ropsten).isChecked = true
                }
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
            adapter = AutoAdapter<RedPacketData>(R.layout.item_red_packet).apply {
                items = viewModel.redPacket
                bindCustom<RedPacketCard>(R.id.red_packet) { view, item, _, _ ->
                    view.data = item
                }
                itemClicked.observe(viewLifecycleOwner, Observer {
                    if (it.item.status != RedPacketStatus.pending && it.item.status != RedPacketStatus.fail) {
                        context.toActivity<RedPacketDetailActivity>(Intent().putExtra("data", it.item))
                    }
                })
                itemLongPressed.observe(viewLifecycleOwner, Observer { args ->
                    when (args.item.status) {
                        RedPacketStatus.normal, RedPacketStatus.incoming -> {
                            PopupMenu(args.view.context, args.view).apply {
                                this.gravity = Gravity.END
                                inflate(R.menu.redpacket_normal)
                                setOnMenuItemClickListener {
                                    when (it.itemId) {
                                        R.id.menu_claim_red_packet -> {
                                            context.toActivity<IncomingRedPacketActivity>(Intent().putExtra("data", args.item))
                                            true
                                        }
                                        R.id.menu_copy_text -> {
                                            context.shareText(RedPacketPayloadHelper.pack(args.item.encPayload!!))
                                            true
                                        }
                                        else -> false
                                    }
                                }
                            }.show()
                        }
                    }
                })
                withHeader(headerView)
            }
        }
        create_redpacket_button.setOnClickListener {
            context.toActivity<SendRedPacketActivity>()
        }
        open_redpacket_button.setOnClickListener {
            context.toActivity<OpenRedPacketActivity>()
        }

        viewModel.wallets.collectionChanged.observe(viewLifecycleOwner, Observer {
            empty_key_container.isVisible = !viewModel.wallets.any()
        })
    }
}