package com.sujitech.tessercubecore.common.wallet

import androidx.lifecycle.MutableLiveData
import com.sujitech.tessercubecore.BuildConfig
import com.sujitech.tessercubecore.common.extension.hexStringToByteArray
import com.sujitech.tessercubecore.data.RedPacketNetwork
import org.web3j.crypto.Hash
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ChainIdLong
import org.web3j.tx.gas.StaticGasProvider
import org.web3j.utils.Convert
import java.math.BigInteger

val blockTime = 15L * 1000L
val retryCount = 10
val defaultGasPrice: BigInteger = Convert.toWei(10.toBigDecimal(), Convert.Unit.GWEI).toBigInteger()
val defaultGasLimit = 1000000.toBigInteger()
val defaultContractVersion = 1
val defaultAESVersion = 1
val defaultRedPacketDuration = 86400L

var currentEthNetworkType = RedPacketNetwork.Rinkeby
    set(value) {
        field = value
        currentEthNetworkLiveData.value = value
    }

val currentEthNetworkLiveData = MutableLiveData(RedPacketNetwork.Rinkeby)

val RedPacketNetwork.url: String
    get() = when (this) {
        RedPacketNetwork.Mainnet -> BuildConfig.MAINNET_ETH_URL
        RedPacketNetwork.Rinkeby -> BuildConfig.RINKEBY_ETH_URL
        RedPacketNetwork.Ropsten -> BuildConfig.ROPSTEN_ETH_URL
    }

val RedPacketNetwork.web3j: Web3j
    get() = Web3j.build(HttpService(this.url))

val RedPacketNetwork.ethChainID: Long
    get() = when (this) {
        RedPacketNetwork.Mainnet -> ChainIdLong.MAINNET
        RedPacketNetwork.Rinkeby -> ChainIdLong.RINKEBY
        RedPacketNetwork.Ropsten -> ChainIdLong.ROPSTEN
    }

val RedPacketNetwork.defaultContractAddress: String
    get() = when (this) {
        RedPacketNetwork.Mainnet -> TODO()
        RedPacketNetwork.Rinkeby -> BuildConfig.RINKEBY_CONTRACT_ADDRESS
        RedPacketNetwork.Ropsten -> BuildConfig.ROPSTEN_CONTRACT_ADDRESS
    }

fun <T> Web3j.use(block: (Web3j) -> T): T {
    val result = block.invoke(this)
    this.shutdown()
    return result
}

fun getDefaultGasProvider() = StaticGasProvider(defaultGasPrice, defaultGasLimit)

fun String.sha3Hex() = Hash.sha3String(this).removePrefix("0x").hexStringToByteArray()

