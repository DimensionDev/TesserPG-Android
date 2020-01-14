package com.sujitech.tessercubecore.common.wallet

import com.sujitech.tessercubecore.BuildConfig
import com.sujitech.tessercubecore.common.extension.hexStringToByteArray
import com.sujitech.tessercubecore.data.*
import io.github.novacrypto.base58.Base58
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.web3j.tx.ChainIdLong
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object RedPacketPayloadHelper {

    private val HEADER = "---Begin Smart Text---" + System.lineSeparator() + "Claim this red packet with tessercube.com" + System.lineSeparator() + System.lineSeparator()
    private val FOOTER = System.lineSeparator() + "---End Smart Text---"

    private val GCM_IV  = BuildConfig.GCM_IV.hexStringToByteArray()
    private val AES_KEY = BuildConfig.AES_KEY.hexStringToByteArray()

    fun isRedPacketPayload(value: String): Boolean {
        return value.trim().startsWith(HEADER) && value.trim().endsWith(FOOTER)
    }

    fun pack(payload: String): String {
        return HEADER + payload.trim() + FOOTER
    }

    fun unpack(payload: String): String {
        return payload.trim().removePrefix(HEADER).removeSuffix(FOOTER)
    }

    fun generate(redPacketData: RedPacketData): String {
        if (redPacketData.status != RedPacketStatus.normal) {
            throw IllegalStateException("Red Packet must be normal state")
        }
        return Json.nonstrict.stringify(RedPacketRawPayload.serializer(), redPacketData.toRawPayload()).let {
            Base58.base58Encode(encrypt(compress(it)))
        }
    }

    fun parse(value: String): RedPacketData {
        val raw = value.let {
            decompress(decrypt(Base58.base58Decode(it)))
        }.let {
            String(it)
        }
        val data = Json.nonstrict.parse(RedPacketRawPayload.serializer(), raw)
        return RedPacketDataEntity().apply {
            this.blockCreationTime = data.creation_time
            this.contractAddress = data.contract_address
            this.duration = data.duration
            this.isRandom = data.is_random
            this.sendMessage = data.sender.message
            this.senderAddress = data.sender.address
            this.senderName = data.sender.name
            this.sendTotal = data.total.toBigDecimal()
            this.uuids = data.passwords.joinToString(";")
            this.contractVersion = data.contract_version
            this.aesVersion = defaultAESVersion//TODO
            this.status = RedPacketStatus.incoming
            this.redPacketId = data.rpid
            this.encPayload = value
            this.rawPayload = raw
        }
    }

    private fun encrypt(byteArray: ByteArray): ByteArray {
        val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider())
        val keySpec = SecretKeySpec(AES_KEY, "AES")
        val gcmParameterSpec = GCMParameterSpec(16 * 8, GCM_IV)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec)
        return cipher.doFinal(byteArray)
    }

    private fun decrypt(byteArray: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider())
        val keySpec = SecretKeySpec(AES_KEY, "AES")
        val gcmParameterSpec = GCMParameterSpec(16 * 8, GCM_IV)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec)
        return cipher.doFinal(byteArray)
    }

    private fun compress(value: String): ByteArray {
        return ByteArrayOutputStream().use { byteArrayOutputStream ->
            GZIPOutputStream(byteArrayOutputStream).use { gzipOutputStream ->
                gzipOutputStream.write(value.toByteArray())
            }
            byteArrayOutputStream.toByteArray()
        }
    }

    private fun decompress(byteArray: ByteArray): ByteArray {
        return byteArray.inputStream().use { byteArrayInputStream ->
            GZIPInputStream(byteArrayInputStream).use { gzipInputStream ->
                ByteArrayOutputStream().use { byteArrayOutputStream ->
                    gzipInputStream.copyTo(byteArrayOutputStream)
                    byteArrayOutputStream.toByteArray()
                }
            }
        }
    }
}


fun RedPacketData.toRawPayload(): RedPacketRawPayload {
    return RedPacketRawPayload(
            contractAddress,
            defaultContractVersion,
            blockCreationTime!!,
            duration,
            isRandom,
            passwords,
            redPacketId!!,
            RedPacketSenderData(
                    senderAddress,
                    sendMessage,
                    senderName
            ),
            sendTotal.toString(),
            network = when (ethChainID) {
                ChainIdLong.RINKEBY -> RedPacketNetwork.Rinkeby
                else -> null
            },
            token = erC20Token?.toTokenData(),
            tokenType = tokenType
    )
}

private fun ERC20Token.toTokenData(): ERC20TokenData {
    return ERC20TokenData(
            address, name, decimals, symbol
    )
}

@Serializable
data class RedPacketRawPayload(
        val contract_address: String,
        val contract_version: Int,
        val creation_time: Long,
        val duration: Long,
        val is_random: Boolean,
        val passwords: List<String>,
        val rpid: String,
        val sender: RedPacketSenderData,
        val total: String,
        val network: RedPacketNetwork? = null,
        @SerialName("token_type")
        val tokenType: RedPacketTokenType,
        val token: ERC20TokenData? = null
)

@Serializable
data class ERC20TokenData(
        val address: String,
        val name: String,
        val decimals: Int,
        val symbol: String
)

@Serializable
data class RedPacketSenderData(
        val address: String,
        val message: String,
        val name: String
)