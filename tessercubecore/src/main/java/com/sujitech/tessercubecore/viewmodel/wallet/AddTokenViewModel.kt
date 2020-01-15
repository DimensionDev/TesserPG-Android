package com.sujitech.tessercubecore.viewmodel.wallet

import androidx.lifecycle.ViewModel
import com.sujitech.tessercubecore.common.collection.ObservableCollection
import com.sujitech.tessercubecore.common.wallet.currentEthNetworkType
import com.sujitech.tessercubecore.data.*
import io.requery.kotlin.eq

class AddTokenViewModel : ViewModel() {

    val tokens by lazy {
        DbContext.data.select(ERC20Token::class).where(ERC20Token::network eq currentEthNetworkType).get().toList().let {
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

    fun addToken(data: WalletData?, item: ERC20Token) {
        if (data == null) {
            return
        }
        val wallet = DbContext.data.select(WalletData::class).where(WalletData::dataId eq data.dataId).get().firstOrNull() ?: return
        if (DbContext.data.select(WalletToken::class).where(WalletToken::wallet eq wallet).and(WalletToken::token eq item).get().any()) {
            return
        }
        val count = DbContext.data.select(WalletToken::class).where(WalletToken::wallet eq wallet).get().count()
        WalletTokenEntity().also {
            it.orderIndex = count
            it.token = item
            it.wallet = wallet
        }.let {
            DbContext.data.insert(it).blockingGet()
        }.let {
            wallet.walletToken.add(it)
            DbContext.data.update(wallet).blockingGet()
        }
    }
}