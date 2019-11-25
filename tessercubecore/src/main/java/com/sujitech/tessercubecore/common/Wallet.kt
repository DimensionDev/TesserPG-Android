package com.sujitech.tessercubecore.common

import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

fun createWeb3j(): Web3j {
    return Web3j.build(HttpService("<TODO>"))  // TODO: Enter your Infura token here;
}