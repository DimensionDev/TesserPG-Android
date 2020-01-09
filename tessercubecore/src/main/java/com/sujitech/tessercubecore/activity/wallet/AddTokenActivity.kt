package com.sujitech.tessercubecore.activity.wallet

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.data.ERC20Token
import com.sujitech.tessercubecore.viewmodel.wallet.AddTokenViewModel
import kotlinx.android.synthetic.main.activity_add_token.*

class AddTokenActivity : BaseActivity() {
    private val viewModel by viewModels<AddTokenViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_token)
        search_bar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.filter(newText)
                return false
            }
        })
        recycler_view.apply {
            layoutManager = LinearLayoutManager(this@AddTokenActivity)
            adapter = AutoAdapter<ERC20Token>(R.layout.item_token).apply {
                items = viewModel.actualTokens
                bindText(R.id.token_name) {
                    it.name
                }
                bindText(R.id.token_symbol) {
                    it.symbol
                }
                itemClicked.observe(this@AddTokenActivity) {

                }
            }
        }
    }
}
