package com.sujitech.tessercubecore.viewmodel.wallet

import android.text.Editable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sujitech.tessercubecore.appContext
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.extension.await
import com.sujitech.tessercubecore.common.wallet.currentEthNetworkType
import com.sujitech.tessercubecore.common.wallet.getDefaultGasProvider
import com.sujitech.tessercubecore.common.wallet.web3j
import com.sujitech.tessercubecore.contracts.generated.ERC20Detailed
import com.sujitech.tessercubecore.data.*
import io.requery.kotlin.eq
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.web3j.crypto.WalletUtils

class AddCustomTokenViewModel : ViewModel() {
    private var currentTask: Deferred<Unit>? = null
    val name = MutableLiveData<String>()
    val decimals = MutableLiveData<Int>()
    val symbol = MutableLiveData<String>()
    val success = MutableLiveData<Boolean>()

    fun getTokenInfo(wallet: WalletData, it: Editable?) {
        success.value = false
        if (it.isNullOrEmpty() || !it.startsWith("0x") || it.length != 42) {
            name.value = ""
            decimals.value = 0
            symbol.value = ""
            return
        }
        val mnemonic = UserPasswordStorage.get(appContext, wallet.mnemonicId)
        val password = UserPasswordStorage.get(appContext, wallet.passwordId)
        val credential = WalletUtils.loadBip39Credentials(password, mnemonic)
        if (currentTask != null) {
            currentTask?.cancel()
        }
        currentTask = viewModelScope.async {
            try {
                val detailed = ERC20Detailed.load(it.toString(), web3j, credential, getDefaultGasProvider())
                val nameResult = detailed.name().sendAsync().await()
                val decimalsResult = detailed.decimals().sendAsync().await().toInt()
                val symbolResult = detailed.symbol().sendAsync().await()
                name.value = nameResult
                decimals.value = decimalsResult
                symbol.value = symbolResult
                success.value = true
            } catch (e: Error) {
                e.printStackTrace()
                // Ignore any error while user input and reset information
                name.value = ""
                decimals.value = 0
                symbol.value = ""
            }
        }
        currentTask?.start()
    }


    suspend fun addToken(data: WalletData?, address: String) {
        if (data == null || address.isNullOrEmpty() || !address.startsWith("0x") || address.length != 42) {
            return
        }
        if (DbContext.data.select(ERC20Token::class).where(ERC20Token::address eq address).get().any()) {
            return
        }
        val wallet = DbContext.data.select(WalletData::class).where(WalletData::dataId eq data.dataId).get().firstOrNull() ?: return
        val mnemonic = UserPasswordStorage.get(appContext, wallet.mnemonicId)
        val password = UserPasswordStorage.get(appContext, wallet.passwordId)
        val credential = WalletUtils.loadBip39Credentials(password, mnemonic)
        val detailed = ERC20Detailed.load(address, web3j, credential, getDefaultGasProvider())
        val nameResult = detailed.name().sendAsync().await()
        val decimalsResult = detailed.decimals().sendAsync().await().toInt()
        val symbolResult = detailed.symbol().sendAsync().await()
        val erC20Token = ERC20TokenEntity().apply {
            this.name = nameResult
            this.symbol = symbolResult
            this.isUserDefine = true
            this.decimals = decimalsResult
            this.address = address
            this.network = currentEthNetworkType
        }.let {
            withContext(Dispatchers.Main) {
                DbContext.data.insert(it).blockingGet()
            }
        }
        val count = DbContext.data.select(WalletToken::class).where(WalletToken::wallet eq wallet).get().count()
        WalletTokenEntity().also {
            it.orderIndex = count
            it.token = erC20Token
            it.wallet = wallet
        }.let {
            withContext(Dispatchers.Main) {
                DbContext.data.insert(it).blockingGet()
                wallet.walletToken.add(it)
                DbContext.data.update(wallet).blockingGet()
            }
        }
    }
}