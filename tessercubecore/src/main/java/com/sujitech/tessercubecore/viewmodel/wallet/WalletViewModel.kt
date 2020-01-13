package com.sujitech.tessercubecore.viewmodel.wallet

import androidx.lifecycle.ViewModel
import com.sujitech.tessercubecore.common.collection.ObservableCollection
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.RedPacketData
import com.sujitech.tessercubecore.data.WalletData
import com.sujitech.tessercubecore.wallet.BalanceUpdater
import com.sujitech.tessercubecore.wallet.RedPacketUpdater
import io.reactivex.disposables.Disposable

class WalletViewModel : ViewModel() {
    private var totalRedPacket = emptyList<RedPacketData>()
    val wallets = ObservableCollection<WalletData>()
    val redPacket = ObservableCollection<RedPacketData>()

    var currentWallet: WalletData? = null
        set(value) {
            field = value
            updateRedPacket()
        }

    private val walletSubscription: Disposable = DbContext.data.select(WalletData::class).get().observableResult().subscribe {
        wallets.clear()
        wallets.addAll(it)
    }

    private val redPacketSubscription: Disposable = DbContext.data.select(RedPacketData::class).get().observableResult().subscribe {
        totalRedPacket = it.toList()
        updateRedPacket()
        RedPacketUpdater.put(totalRedPacket)
    }

    init {
        DbContext.data.select(WalletData::class).get().forEach {
            BalanceUpdater.update(it)
        }
    }

    private fun updateRedPacket() {
        currentWallet?.let { value ->
            if (redPacket.any()) {
                redPacket.clear()
            }
            redPacket.addAll(totalRedPacket.filter {
                it.senderAddress == value.address || it.claimAddress == value.address
            }.reversed())
        }
    }

    override fun onCleared() {
        super.onCleared()
        walletSubscription.dispose()
        redPacketSubscription.dispose()
    }
}