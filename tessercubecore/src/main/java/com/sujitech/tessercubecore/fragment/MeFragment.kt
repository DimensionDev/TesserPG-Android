package com.sujitech.tessercubecore.fragment


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.SettingsActivity
import com.sujitech.tessercubecore.activity.keypair.CreateKeyActivity
import com.sujitech.tessercubecore.activity.keypair.ImportKeyActivity
import com.sujitech.tessercubecore.common.Settings
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.adapter.updateItemsSource
import com.sujitech.tessercubecore.common.extension.*
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.UserKeyData
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_me.*
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import uk.co.samuelwall.materialtaptargetprompt.extras.backgrounds.RectanglePromptBackground
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal


class MeFragment : ViewPagerFragment() {

    private val QR_CODE_INTENT_REQUESTCODE = 431

    private lateinit var addPopupMenu: PopupMenu

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_me, container, false)
    }

    private lateinit var userKeySubscription: Disposable

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
                                        context.toActivity<CreateKeyActivity>()
                                        true
                                    }
                                    R.id.menu_import_key -> {
                                        context.toActivity<ImportKeyActivity>()
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
        settings_button.setOnClickListener {
//            startActivity(Intent(context, Class.forName("com.android.inputmethod.latin.settings.SettingsActivity")))
            context.toActivity<SettingsActivity>()
        }
        scan_qr_code_button.setOnClickListener {
            kotlin.runCatching {
                val intent = Intent("com.google.zxing.client.android.SCAN")
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE") // "PRODUCT_MODE for bar codes
                startActivityForResult(intent, QR_CODE_INTENT_REQUESTCODE)
            }.onFailure {
                Toast.makeText(context, "No QR Scanner found!", Toast.LENGTH_SHORT).show()
            }
        }
        create_key_button.setOnClickListener {
            context.toActivity<CreateKeyActivity>()
        }
        import_key_button.setOnClickListener {
            context.toActivity<ImportKeyActivity>()
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
                itemClicked.observe(viewLifecycleOwner, Observer { args ->
                    PopupMenu(context, args.view).apply {
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
                                    revokeUserKey(args.item)
                                    true
                                }
                                R.id.menu_delete -> {
                                    deleteUserKey(args.item)
                                    true
                                }
                                else -> false
                            }
                        }
                    }.show()
                })
            }
        }
        userKeySubscription = DbContext.data.select(UserKeyData::class).get().observableResult().subscribe {
            val result = it.toList()
            updateUserKeyData(result)
        }
    }


    private fun revokeUserKey(item: UserKeyData) {

    }

    private fun deleteUserKey(item: UserKeyData) {
        DbContext.data.delete(item).blockingGet()
    }

    private fun updateUserKeyData(result: List<UserKeyData>) {
        empty_key_container.isVisible = !result.any()
        recycler_view.updateItemsSource(result)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!userKeySubscription.isDisposed) {
            userKeySubscription.dispose()
        }
    }

    override fun onPageSelected() {
        super.onPageSelected()
        if (!Settings.get("has_me_entry", false)) {
            activity?.let {
                MaterialTapTargetPrompt.Builder(it)
                        .setTarget(create_key_button)
                        .setPromptBackground(RectanglePromptBackground())
                        .setPromptFocal(RectanglePromptFocal())
                        .setPrimaryText(getString(R.string.intro_create_key_title))
                        .setSecondaryText(getString(R.string.intro_create_key_desc))
                        .setPromptStateChangeListener { prompt, state ->
                            Settings.set("has_me_entry", true)
                        }
                        .show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == QR_CODE_INTENT_REQUESTCODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val contents = data.getStringExtra("SCAN_RESULT")
                Toast.makeText(context, contents, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
