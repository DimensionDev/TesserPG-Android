package com.sujitech.tessercubecore.activity.wallet

import android.os.Bundle
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.adapter.getItemsSource
import com.sujitech.tessercubecore.common.adapter.updateItemsSource
import com.sujitech.tessercubecore.common.extension.toast
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.WalletDataEntity
import kotlinx.android.synthetic.main.activity_import_wallet.*
import org.web3j.crypto.MnemonicUtils
import org.web3j.crypto.WalletUtils
import java.util.*

class ImportWalletActivity : BaseActivity() {

    private data class Words(
            var value: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_wallet)
        recycler_view.adapter = AutoAdapter<Words>(R.layout.item_mnemonic_code_input).apply {
            bindCustom<EditText>(android.R.id.text1) { view, item, _, _ ->
                view.doOnTextChanged { text, _, _, _ ->
                    item.value = text.toString()
                }
            }
        }
        recycler_view.updateItemsSource((0 until 12).map { Words() })
        confirm_button.setOnClickListener {
            recycler_view.getItemsSource<Words>()?.let {
                it.joinToString(" ")
            }?.takeIf {
                MnemonicUtils.validateMnemonic(it)
            }?.let { mnemonic ->
                val password = password_input.text.toString()
                kotlin.runCatching {
                    WalletUtils.loadBip39Credentials(password, mnemonic)
                }.onFailure {
                    //todo
                    toast(getString(R.string.error_import_private_key))
                }.getOrNull()?.let {
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
                }
            } ?: toast(getString(R.string.error_import_private_key))
        }
    }

}
