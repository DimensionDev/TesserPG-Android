package com.sujitech.tessercubecore.activity.wallet

import android.os.Bundle
import androidx.recyclerview.widget.ItemTouchHelper
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.SimpleItemTouchHelperCallback
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.adapter.getItemsSource
import com.sujitech.tessercubecore.common.adapter.updateItemsSource
import com.sujitech.tessercubecore.common.extension.toast
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.WalletDataEntity
import kotlinx.android.synthetic.main.activity_mnemonic_code_confirm.*
import org.web3j.crypto.WalletUtils
import java.util.*

class MnemonicCodeConfirmActivity : BaseActivity() {


    private val password by lazy {
        intent.getStringExtra("password")
    }

    private val mnemonic by lazy {
        intent.getStringExtra("mnemonic")
    }


    private val touchHelper by lazy {
        ItemTouchHelper(SimpleItemTouchHelperCallback { fromPosition, toPosition ->
            result_recycler_view.getItemsSource<String>()?.let {
                Collections.swap(it, fromPosition, toPosition)
            }
            result_recycler_view.adapter?.notifyItemMoved(fromPosition, toPosition)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mnemonic_code_confirm)


        tap_recycler_view.adapter = AutoAdapter<String>(R.layout.item_mnemonic_code).apply {
            bindText(android.R.id.text1) {
                it
            }
            itemClicked.observe(this@MnemonicCodeConfirmActivity, androidx.lifecycle.Observer { args ->
                result_recycler_view.getItemsSource<String>()?.add(args.item)
                items.remove(args.item)
            })
        }

        result_recycler_view.adapter = AutoAdapter<String>(R.layout.item_mnemonic_code).apply {
            bindText(android.R.id.text1) {
                it
            }
            itemClicked.observe(this@MnemonicCodeConfirmActivity, androidx.lifecycle.Observer { args ->
                tap_recycler_view.getItemsSource<String>()?.add(args.item)
                items.remove(args.item)
            })
        }

        touchHelper.attachToRecyclerView(result_recycler_view)
        tap_recycler_view.updateItemsSource(mnemonic?.split(" "))

        confirm_button.setOnClickListener {
            onConfirm()
        }
    }

    private fun onConfirm() {
        result_recycler_view.getItemsSource<String>()?.let {
            it.joinToString(" ")
        }?.takeIf {
            it == mnemonic
        }?.let {
            WalletUtils.loadBip39Credentials(password, mnemonic)
        }?.let {
            val passwordId = UUID.randomUUID().toString()
            UserPasswordStorage.save(this, passwordId, password)
            val mnemonicId = UUID.randomUUID().toString()
            UserPasswordStorage.save(this, mnemonicId, mnemonic)
            WalletDataEntity().also { wallet ->
                wallet.passwordId = passwordId
                wallet.mnemonicId = mnemonicId
                wallet.address = it.address
            }
        }?.let {
            runOnUiThread {
                DbContext.data.insert(it).blockingGet()
            }
            finish()
        } ?: toast(getString(R.string.error_create_key))
    }
}
