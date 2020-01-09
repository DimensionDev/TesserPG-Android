package com.sujitech.tessercubecore.viewmodel.wallet

import androidx.lifecycle.ViewModel
import com.sujitech.tessercubecore.common.collection.ObservableCollection
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.ERC20Token

class AddTokenViewModel : ViewModel() {

    val tokens by lazy {
        DbContext.data.select(ERC20Token::class).get().toList().let {
            ArrayList(it)
        }
    }

    val actualTokens by lazy {
        ObservableCollection<ERC20Token>().apply {
            addAll(tokens)
        }
    }

    fun filter(newText: String?) {
        if (newText.isNullOrEmpty()) {
            if (actualTokens.size != tokens.size) {
                actualTokens.clear()
                actualTokens.addAll(tokens)
            }
        } else {
            actualTokens.clear()
            actualTokens.addAll(tokens.filter { it.symbol.contains(newText, true) || it.name.contains(newText, true) })
        }
    }
}