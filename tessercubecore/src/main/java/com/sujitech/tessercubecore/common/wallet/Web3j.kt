package com.sujitech.tessercubecore.common.wallet

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
/**
 * @see org.web3j.tx.ChainIdLong
 */
val ethChainID = BuildConfig.NET_TYPE.toLong()
val currentEthNetworkType = when (ethChainID) {
    ChainIdLong.RINKEBY -> RedPacketNetwork.Rinkeby
    ChainIdLong.MAINNET -> RedPacketNetwork.Mainnet
    else -> TODO()
}
val redPacketContractAddress = BuildConfig.CONTRACT_ADDRESS
val ethUrl = BuildConfig.ETH_URL
val web3j by lazy {
    Web3j.build(HttpService(ethUrl))
}
fun getDefaultGasProvider() = StaticGasProvider(defaultGasPrice, defaultGasLimit)

fun String.sha3Hex() = Hash.sha3String(this).removePrefix("0x").hexStringToByteArray()
