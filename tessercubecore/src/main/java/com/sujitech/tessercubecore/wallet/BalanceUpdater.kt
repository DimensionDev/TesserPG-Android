package com.sujitech.tessercubecore.wallet

import com.sujitech.tessercubecore.appContext
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.extension.await
import com.sujitech.tessercubecore.common.wallet.getDefaultGasProvider
import com.sujitech.tessercubecore.common.wallet.web3j
import com.sujitech.tessercubecore.contracts.generated.IERC20
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.WalletData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import java.util.concurrent.LinkedBlockingDeque

object BalanceUpdater {
    private val queue = LinkedBlockingDeque<WalletData>()
    private val taskCreator = {
        GlobalScope.async {
            while (!queue.isEmpty()) {
                val wallet = queue.take()
                wallet.balance = web3j.ethGetBalance(wallet.address, DefaultBlockParameterName.LATEST).sendAsync().await().balance.toBigDecimal()
                val mnemonic = UserPasswordStorage.get(appContext, wallet.mnemonicId)
                val password = UserPasswordStorage.get(appContext, wallet.passwordId)
                val credential = WalletUtils.loadBip39Credentials(password, mnemonic)
                wallet.walletToken.forEach {
                    try {
                        it.tokenBalance = IERC20.load(
                                it.token.address,
                                web3j,
                                credential,
                                getDefaultGasProvider()
                        ).balanceOf(wallet.address).sendAsync().await().toBigDecimal()
                    } catch (e: Error) {
                        e.printStackTrace()
                    }
                }
                withContext(Dispatchers.Main) {
                    DbContext.data.update(wallet).blockingGet()
                }
            }
        }
    }

    private var task = taskCreator.invoke()

    fun update(data: WalletData) {
        queue.add(data)
        if (task.isCompleted) {
            task = taskCreator.invoke()
        }
        if (!task.isActive) {
            task.start()
        }
    }


}