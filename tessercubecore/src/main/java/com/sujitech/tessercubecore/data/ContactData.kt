package com.sujitech.tessercubecore.data

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcelable
import com.tylersuehr.chips.Chip
import io.requery.*
import moe.tlaster.kotlinpgp.data.VerifyStatus
import java.math.BigDecimal
import java.util.*

enum class TrustLevel {
    Unknown,
    Not,
    Marginally,
    Fully,
}


@Entity
abstract class ContactData : Persistable, Parcelable, Chip() {
    @Transient
    override fun getSubtitle(): String? {
        return keyData.firstOrNull()?.keyId?.toString(16)?.takeLast(8)?.toUpperCase()
    }

    @Transient
    override fun getAvatarDrawable(): Drawable? {
        return null
    }

    @Transient
    override fun getId(): Any? {
        return null
    }

    @Transient
    override fun getTitle(): String {
        return name
    }

    @Transient
    override fun getAvatarUri(): Uri? {
        return null
    }

    @get:Key
    @get:Generated
    abstract val dataId: Int
    abstract var name: String
    abstract var email: String
    abstract var pubKeyContent: String
    //Master key id
    abstract var keyId: Long
    abstract var trustLevel: TrustLevel
    abstract var isUserKey: Boolean
    @get:ForeignKey
    @get:OneToOne
    abstract var userKeyData: UserKeyData
    @get:OneToMany
    abstract val keyData: MutableList<KeyData>
}

@Entity
interface KeyData: Persistable, Parcelable {
    @get:Key
    @get:Generated
    val dataId: Int
    var keyId: Long
    var createAt: Date
    var algorithm: Int
    var bitStrength: Int
    var validSeconds: Long
    var fingerPrint: String
    var isMasterKey: Boolean
    var isEncryptionKey: Boolean
    var isSigningKey: Boolean
    @get:ForeignKey
    @get:ManyToOne
    var contactData: ContactData
}

@Entity
interface WalletData : Persistable, Parcelable {
    @get:Key
    @get:Generated
    val dataId: Int
    var address: String
    var passwordId: String
    var mnemonicId: String
    var balance: BigDecimal?
}

@Entity
interface UserKeyData : Persistable, Parcelable {
    @get:Key
    @get:Generated
    val dataId: Int
    @get:OneToOne(mappedBy = "userKeyData")
    var contactData: ContactData?
    var priKey: String
    var hasPassword: Boolean
    var uuid: String
}

@Entity
interface MessageUserData : Persistable, Parcelable {
    @get:Key
    @get:Generated
    val dataId: Int
    var name: String?
    var email: String?
    var keyId: Long?
    @get:ForeignKey
    @get:OneToOne
    var messageDataFrom: MessageData
    @get:ForeignKey
    @get:ManyToOne(cascade = [CascadeAction.NONE])
    var messageDataTo: MessageData
}

@Entity
interface RedPacketData : Persistable, Parcelable {
    @get:Key
    @get:Generated
    val dataId: Int
    @get:ForeignKey
    @get:OneToOne
    val messageData: MessageData
    var shares: Int
    var price: BigDecimal
    var fromMe: Boolean
    var state: RedPacketState
    var collectedCount: Int?
    var remainPrice: BigDecimal?
}

enum class RedPacketState {
    unknown,
    notClaimed,
    claimed,
    claimFailed,
    claimLate,
}

@Entity
interface MessageData : Persistable, Parcelable {
    @get:Key
    @get:Generated
    val dataId: Int
    @get:OneToOne(mappedBy = "messageDataFrom")
    var messageFrom: MessageUserData?
    @get:OneToMany(mappedBy = "messageDataTo")
    val messageTo: MutableList<MessageUserData>
    @get:OneToOne
    var redPacketData: RedPacketData?
    var content: String
    //PGP content
    var rawContent: String
    var composeTime: Date?
    var interpretTime: Date?
    var fromMe: Boolean
    var isDraft: Boolean
    var verifyStatus: VerifyStatus
}

