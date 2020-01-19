package com.sujitech.tessercubecore.wallet

import android.util.Log
import com.sujitech.tessercubecore.appContext
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.extension.await
import com.sujitech.tessercubecore.common.wallet.currentEthNetworkType
import com.sujitech.tessercubecore.common.wallet.getDefaultGasProvider
import com.sujitech.tessercubecore.common.wallet.web3j
import com.sujitech.tessercubecore.contracts.generated.IERC20
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.WalletData
import com.sujitech.tessercubecore.data.updateBalance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.core.DefaultBlockParameterName
import java.util.concurrent.LinkedBlockingDeque

object BalanceUpdater {
    private val TAG = "BalanceUpdater"
    private val queue = LinkedBlockingDeque<WalletData>()
    private val taskCreator = {
        GlobalScope.async {
            Log.i(TAG, "Updating balance")
            while (!queue.isEmpty()) {
                val wallet = queue.take()
                val network = currentEthNetworkType
                val web3j = network.web3j
                val balance = web3j.ethGetBalance(wallet.address, DefaultBlockParameterName.LATEST).sendAsync().await().balance.toBigDecimal()
                Log.i(TAG, "Wallet: ${wallet.address}: ${balance}")
                wallet.updateBalance(network, balance)
                val mnemonic = UserPasswordStorage.get(appContext, wallet.mnemonicId)
                val password = UserPasswordStorage.get(appContext, wallet.passwordId)
                val credential = WalletUtils.loadBip39Credentials(password, mnemonic)
                wallet.walletToken.filter { it.token.network == network }.forEach {
                    try {
                        it.tokenBalance = IERC20.load(
                                it.token.address,
                                web3j,
                                credential,
                                getDefaultGasProvider()
                        ).balanceOf(wallet.address).sendAsync().await().toBigDecimal()
                        Log.i(TAG, "Wallet: ${wallet.address} - ${it.token.symbol}: ${it.tokenBalance}")
                    } catch (e: Error) {
                        e.printStackTrace()
                    }
                }
                withContext(Dispatchers.Main) {
                    DbContext.data.update(wallet).blockingGet()
                }
                web3j.shutdown()
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