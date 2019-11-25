package com.sujitech.tessercubecore.common

import android.content.Context
import com.sujitech.tessercubecore.data.*
import moe.tlaster.kotlinpgp.KotlinPGP
import moe.tlaster.kotlinpgp.data.DecryptResult
import moe.tlaster.kotlinpgp.data.PrivateKeyData
import moe.tlaster.kotlinpgp.data.VerifyStatus
import java.util.*

class PrivateKeyNotFoundError : Throwable()

object MessageDataUtils {
    fun getMessageDataFromEncryptedContent(context: Context, pgpContent: String): MessageData? {
        val decryptResult: DecryptResult?
        val info = KotlinPGP.getEncryptedPackageInfo(pgpContent)
        if (info.isClearSign) {
            decryptResult = KotlinPGP.decrypt("", "", pgpContent)
        } else if (info.containKeys.any()) {
            val allKeys = DbContext.data.select(UserKeyData::class).get().toList()
            val possibleUserKeyData = allKeys.filter {
                it.contactData != null && it.contactData!!.keyData.any { key ->
                    info.containKeys.contains(key.keyId)
                }
            }
            if (possibleUserKeyData.any()) {
                var result: DecryptResult? = null
                for (data in possibleUserKeyData) {
                    val password = if (data.hasPassword) {
                        UserPasswordStorage.get(context, data.uuid)
                                ?: ""
                    } else {
                        ""
                    }
                    kotlin.runCatching {
                        result = KotlinPGP.decrypt(data.priKey, password, pgpContent)
                    }.onFailure {

                    }
                    if (result != null) {
                        break
                    }
                }
                decryptResult = result
            } else if (info.containKeys.contains(0L)) {
                decryptResult = KotlinPGP.tryDecrypt(allKeys.map {
                    PrivateKeyData(it.priKey, if (it.hasPassword) {
                        UserPasswordStorage.get(context, it.uuid)
                                ?: ""
                    } else {
                        ""
                    })
                }, pgpContent)
            } else {
                //TODO: Decrypt anyway?
                throw PrivateKeyNotFoundError()
            }
        } else {
            decryptResult = null
        }
        if (decryptResult != null) {
            val contacts = DbContext.data.select(ContactData::class).get().toList()
            return if (decryptResult.hasSignature) {
                val publicKeys = contacts.map { it.pubKeyContent }.toList()
                val verifyResult = KotlinPGP.verify(decryptResult.signatureData, publicKeys)

                val from = when (verifyResult.verifyStatus) {
                    VerifyStatus.NO_SIGNATURE -> {
                        //WHAT?
                        throw AssertionError("This is a bug!")
                    }
                    VerifyStatus.SIGNATURE_BAD -> {
                        MessageUserDataEntity().apply {
                            keyId = verifyResult.keyID
                        }
                    }
                    VerifyStatus.SIGNATURE_OK -> {
                        MessageUserDataEntity().apply {
                            contacts.first {
                                it.keyId == verifyResult.keyID
                            }.let {
                                name = it.name
                                email = it.email
                                keyId = it.keyId
                            }
                        }
                    }
                    VerifyStatus.UNKNOWN_PUBLIC_KEY -> {
                        MessageUserDataEntity().apply {
                            keyId = verifyResult.keyID
                        }
                    }
                }

                MessageDataEntity().apply {
                    redPacketData = parseRedPacket(decryptResult.result)
                    content = decryptResult.result
                    rawContent = pgpContent
                    composeTime = decryptResult.time
                    interpretTime = Date()
                    messageFrom = from
                    messageTo.addAll(decryptResult.includedKeys.map { keyId ->
                        contacts.firstOrNull { contact ->
                            contact.keyData.any { key ->
                                key.keyId == keyId
                            }
                        }?.let {
                            MessageUserDataEntity().apply {
                                name = it.name
                                email = it.email
                                this.keyId = it.keyId
                            }
                        } ?: MessageUserDataEntity().also {
                            it.keyId = keyId
                        }
                    })
                    fromMe = false
                    isDraft = false
                    verifyStatus = verifyResult.verifyStatus
                }
            } else {
                MessageDataEntity().apply {
                    redPacketData = parseRedPacket(decryptResult.result)
                    content = decryptResult.result
                    rawContent = pgpContent
                    composeTime = decryptResult.time
                    interpretTime = Date()
                    messageTo.addAll(decryptResult.includedKeys.map { keyId ->
                        contacts.firstOrNull { contact ->
                            contact.keyData.any { key ->
                                key.keyId == keyId
                            }
                        }?.let {
                            MessageUserDataEntity().apply {
                                name = it.name
                                email = it.email
                                this.keyId = it.keyId
                            }
                        } ?: MessageUserDataEntity().also {
                            it.keyId = keyId
                        }
                    })
                    fromMe = false
                    isDraft = false
                    verifyStatus = VerifyStatus.NO_SIGNATURE
                }
            }
        }
        return null
    }

    private fun parseRedPacket(content: String) : RedPacketData? {
        if (!RedPacketUtils.check(content)) {
            return null
        }
        val info = RedPacketUtils.parse(content)
        return RedPacketDataEntity().apply {
            fromMe = false
            state = RedPacketState.notClaimed
            shares = info.uuids.count()
        }
    }
}