package com.sujitech.tessercubecore.viewmodel.wallet

import androidx.lifecycle.ViewModel
import com.sujitech.tessercubecore.common.collection.ObservableCollection
import com.sujitech.tessercubecore.common.extension.format
import com.sujitech.tessercubecore.data.*
import io.reactivex.disposables.Disposable
import io.requery.kotlin.eq
import org.web3j.utils.Convert

class TokenSelectViewModel : ViewModel() {
    private var walletSubscription: Disposable? = null
    private var currentFilter: String? = null
    private var tokenSubscription: Disposable? = null
    val tokens = arrayListOf<WalletToken>()

    val actualTokens by lazy {
        ObservableCollection<WalletToken>().apply {
            addAll(tokens)
        }
    }

    fun filter(newText: String?) {
        currentFilter = newText
        if (newText.isNullOrEmpty()) {
            if (actualTokens.size != tokens.size) {
                actualTokens.clear()
                actualTokens.addAll(tokens)
            }
        } else {
            actualTokens.clear()
            actualTokens.addAll(tokens.filter { it.token.symbol.contains(newText, true) || it.token.name.contains(newText, true) })
        }
    }

    fun init(data: WalletData?) {
        if (data == null) {
            return
        }
        val wallet = DbContext.data.select(WalletData::class).where(WalletData::dataId eq data.dataId)
                .get().firstOrNull() ?: return
        walletSubscription = DbContext.data.select(WalletData::class).where(WalletData::dataId eq data.dataId).get().observableResult().subscribe {
            if (tokens.any()) {
                tokens.removeAt(0)
                tokens.add(0, WalletTokenEntity().apply {
                    this.wallet = data
                    this.tokenBalance = Convert.fromWei(data.balance, Convert.Unit.ETHER).format(4).toBigDecimal()
                    this.token = ERC20TokenEntity().apply {
                        this.symbol = "ETH"
                        this.name = "ETH"
                    }
                })
            }
            filter(currentFilter)
        }
        tokenSubscription = DbContext.data.select(WalletToken::class).where(WalletToken::wallet eq wallet).get().observableResult().subscribe {
            tokens.clear()
            tokens.add(WalletTokenEntity().apply {
                this.wallet = data
                this.tokenBalance = Convert.fromWei(data.balance, Convert.Unit.ETHER).format(4).toBigDecimal()
                this.token = ERC20TokenEntity().apply {
                    this.symbol = "ETH"
                    this.name = "ETH"
                }
            })
            tokens.addAll(it)
            filter(currentFilter)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tokenSubscription?.dispose()
        walletSubscription?.dispose()
    }
}