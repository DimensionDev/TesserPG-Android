package com.sujitech.tessercubecore.viewmodel.wallet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sujitech.tessercubecore.common.collection.ObservableCollection
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.WalletData
import com.sujitech.tessercubecore.data.WalletToken
import com.sujitech.tessercubecore.wallet.BalanceUpdater
import io.reactivex.disposables.Disposable
import io.requery.kotlin.eq

class WalletDetailViewModel : ViewModel() {

    val tokens = ObservableCollection<WalletToken>()
    val wallet = MutableLiveData<WalletData>()

    private lateinit var walletSubscription: Disposable
    private lateinit var subscription: Disposable

    fun loadToken(data: WalletData?) {
        if (data == null) {
            return
        }
        walletSubscription = DbContext.data.select(WalletData::class).where(WalletData::dataId eq data.dataId).get().observableResult().subscribe {
            wallet.value = it.first()
        }
        BalanceUpdater.update(DbContext.data.select(WalletData::class).where(WalletData::dataId eq data.dataId).get().first())
        subscription = DbContext.data.select(WalletToken::class).where(WalletToken::wallet eq data).get().observableResult().subscribe {
            tokens.clear()
            tokens.addAll(it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        subscription.dispose()
        walletSubscription.dispose()
    }
}