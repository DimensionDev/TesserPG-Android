package com.sujitech.tessercubecore.common.wallet

import org.web3j.crypto.Hash
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ChainIdLong
import org.web3j.tx.gas.StaticGasProvider
import org.web3j.utils.Convert
import java.math.BigInteger

val defaultGasPrice: BigInteger = Convert.toWei(10.toBigDecimal(), Convert.Unit.GWEI).toBigInteger()
val defaultGasLimit = 1000000.toBigInteger()
val defaultContractVersion = 1
val defaultAESVersion = 1
val defaultRedPacketDuration = 86400L
val ethChainID = ChainIdLong.RINKEBY
val redPacketContractAddress = ""
val ethUrl = ""
fun createWeb3j() = Web3j.build(HttpService(ethUrl))

fun getDefaultGasProvider() = StaticGasProvider(defaultGasPrice, defaultGasLimit)

fun String.sha3Hex() = Hash.sha3String(this).removePrefix("0x").hexStringToByteArray()

fun String.hexStringToByteArray() = ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
