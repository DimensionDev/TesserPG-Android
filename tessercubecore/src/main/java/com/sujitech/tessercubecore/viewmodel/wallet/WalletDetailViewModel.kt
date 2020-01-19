package com.sujitech.tessercubecore.viewmodel.wallet

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sujitech.tessercubecore.common.collection.ObservableCollection
import com.sujitech.tessercubecore.common.wallet.currentEthNetworkType
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

    fun loadToken(data: WalletData?) {
        if (data == null) {
            return
        }
        walletSubscription = DbContext.data.select(WalletData::class).where(WalletData::dataId eq data.dataId).get().observableResult().subscribe {
            wallet.value = it.first()
            if (tokens.count() != it.first().walletToken.count() || tokens.count() == 0) {
                BalanceUpdater.update(DbContext.data.select(WalletData::class).where(WalletData::dataId eq data.dataId).get().first())
            }
            tokens.clear()
            tokens.addAll(it.first().walletToken.filter { it.token.network == currentEthNetworkType })
        }
    }

    override fun onCleared() {
        super.onCleared()
        walletSubscription.dispose()
    }

    fun deleteToken(item: WalletToken) {
        item.wallet.walletToken.remove(item)
        DbContext.data.update(item.wallet).blockingGet()
        DbContext.data.delete(item).blockingGet()
    }
}