package com.sujitech.tessercubecore.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.wallet.CreateWalletActivity
import com.sujitech.tessercubecore.activity.wallet.ImportWalletActivity
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.extension.*
import com.sujitech.tessercubecore.data.UserKeyData
import kotlinx.android.synthetic.main.fragment_wallet.*

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
            adapter = AutoAdapter<UserKeyData>(R.layout.item_me_key).apply {
                bindText(R.id.item_key_name) {
                    it.contactData?.name ?: "[non]"
                }
                bindText(R.id.item_key_fingerprint) {
                    it.contactData?.keyData?.firstOrNull()?.fingerPrint?.toFormattedHexText()?.splitTo(5) ?: ""
                }
                bindText(R.id.item_key_type) {
                    it.type
                }
                itemClicked += { sender, args ->
                    PopupMenu(context, sender as View).apply {
                        this.gravity = Gravity.END
                        inflate(R.menu.me_user_key_recycler_view)
                        setOnMenuItemClickListener {
                            when (it.itemId) {
                                R.id.menu_share_public_key -> {
                                    context.shareText(args.item.contactData?.pubKeyContent ?: "")
                                    true
                                }
                                R.id.menu_export_private_key -> {
                                    context.shareText(args.item.priKey)
                                    true
                                }
                                R.id.menu_revoke -> {
//                                    revokeUserKey(args.item)
                                    true
                                }
                                R.id.menu_delete -> {
//                                    deleteUserKey(args.item)
                                    true
                                }
                                else -> false
                            }
                        }
                    }.show()
                }
            }
        }
    }
}