package com.sujitech.tessercubecore.viewmodel.wallet

import androidx.lifecycle.ViewModel
import com.sujitech.tessercubecore.common.collection.ObservableCollection
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.WalletData
import com.sujitech.tessercubecore.data.WalletToken
import io.reactivex.disposables.Disposable
import io.requery.kotlin.eq

class WalletDetailViewModel : ViewModel() {

    val tokens = ObservableCollection<WalletToken>()

    private lateinit var subscription: Disposable

    fun loadToken(data: WalletData?) {
        if (data == null) {
            return
        }
        subscription = DbContext.data.select(WalletToken::class).where(WalletToken::wallet eq data).get().observableResult().subscribe {
            tokens.clear()
            tokens.addAll(it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        subscription.dispose()
    }
}