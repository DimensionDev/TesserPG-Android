package com.sujitech.tessercubecore.activity.wallet

import android.os.Bundle
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.adapter.updateItemsSource
import kotlinx.android.synthetic.main.activity_import_wallet.*

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

        }
    }

}
