package com.sujitech.tessercubecore.common.extension

import android.content.res.Resources
import android.util.TypedValue
import com.sujitech.tessercubecore.data.*
import kotlinx.coroutines.Deferred
import moe.tlaster.kotlinpgp.*
import moe.tlaster.kotlinpgp.data.VerifyStatus
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.util.encoders.Hex
import org.web3j.utils.Convert
import java.util.*


suspend fun <T> await(block: () -> Deferred<T>): T {
    return block().await()
}

val Number.dp: Int
    get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            Resources.getSystem().displayMetrics
    ).toInt()

fun PGPPublicKeyRing.toContactData(pubKey: String): ContactData {
    return publicKey.toContactData(pubKey).apply {
        isUserKey = false
        trustLevel = TrustLevel.Marginally
        val keys = arrayListOf<KeyData>()
        publicKeys.forEach {
            keys.add(it.toKeyData())
        }
        keyData.addAll(keys)
    }
}

fun PGPPublicKey.toContactData(pubKey: String): ContactDataEntity {
    return ContactDataEntity().apply {
        name = this@toContactData.name
        email = this@toContactData.email
        this.pubKeyContent = pubKey
        this.keyId = this@toContactData.keyID
        trustLevel = TrustLevel.Marginally
    }
}

fun PGPPublicKey.toKeyData(): KeyData {
    return KeyDataEntity().also {
        it.keyId = keyID
        it.isSigningKey = false
        it.isEncryptionKey = isEncryptionKey
        it.isMasterKey = isMasterKey
        it.algorithm = algorithm
        it.createAt = creationTime
        it.bitStrength = bitStrength
        it.validSeconds = validSeconds
        it.fingerPrint = Hex.toHexString(fingerprint)
    }
}

fun PGPSecretKeyRing.toUserKeyData(privateKeyStr: String, password: String): UserKeyDataEntity {
    val pgpPublicKeyRing = KotlinPGP.getSecretKeyRingFromString(privateKeyStr, password).extractPublicKeyRing()
    return UserKeyDataEntity().apply {
        priKey = privateKeyStr
        hasPassword = password.isNotEmpty()
        contactData = pgpPublicKeyRing.toContactData(pgpPublicKeyRing.exportToString()).apply {
            trustLevel = TrustLevel.Fully
            isUserKey = true
        }
        uuid = UUID.randomUUID().toString()
    }
}

fun MessageData.applyMessageData(message: String, result: String, sendFromContactData: ContactData?, sendTo: List<ContactData>) {
    content = message
    rawContent = result
    composeTime = Date()
    interpretTime = Date()
    if (sendFromContactData != null) {
        messageFrom = (messageFrom ?: MessageUserDataEntity()).apply {
            name = sendFromContactData.name
            email = sendFromContactData.email
            keyId = sendFromContactData.keyId
        }
    }
    messageTo.clear()
    messageTo.addAll(sendTo.map {
        MessageUserDataEntity().apply {
            name = it.name
            email = it.email
            keyId = it.keyId
        }
    })
    fromMe = true
    isDraft = false
    verifyStatus = VerifyStatus.SIGNATURE_OK
}

val ContactData.isExpired: Boolean
    get() {
        val creationTime = keyData.firstOrNull()?.createAt
        val expiryTime = expiryTime
        val now = Date()
        return creationTime != null && (creationTime.after(now) || (expiryTime != null && expiryTime.before(now)))
    }

val ContactData.expiryTime: Date?
    get() {
        val seconds = keyData.firstOrNull()?.validSeconds
        if (seconds == null || seconds == 0L) {
            return null
        }
        if (seconds > Integer.MAX_VALUE) {
            return null
        }
        return GregorianCalendar.getInstance().apply {
            time = keyData.firstOrNull()?.createAt
            add(Calendar.SECOND, seconds.toInt())
        }.time
    }

val ContactData.type: String
    get() {
        val data = keyData.firstOrNull()
        return if (data != null) {
            getType(data.algorithm, data.bitStrength)
        } else {
            ""
        }
    }
val UserKeyData.type: String
    get() = contactData?.type ?: ""

private fun getType(algorithm: Int, bitStrength: Int): String {
    val pref = when (algorithm) {
        PublicKeyAlgorithmTags.RSA_GENERAL,
        PublicKeyAlgorithmTags.RSA_ENCRYPT,
        PublicKeyAlgorithmTags.RSA_SIGN -> "RSA"
        PublicKeyAlgorithmTags.DSA -> "DSA"
        PublicKeyAlgorithmTags.ECDSA -> "ECDSA"
        PublicKeyAlgorithmTags.ECDH -> "ECDH"
        PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT,
        PublicKeyAlgorithmTags.ELGAMAL_GENERAL -> "Elgamal"
        else -> "ECC"
    }
    val sub = bitStrength.toString()
    return "$pref-$sub"
}

@ExperimentalUnsignedTypes
fun ByteArray.toHexString() = asUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }

fun Number.formatWei(digits: Int = 4) = Convert.fromWei(this.toString(), Convert.Unit.ETHER)

fun Number.format(digits: Int = 4) = "%.${digits}f".format(this)