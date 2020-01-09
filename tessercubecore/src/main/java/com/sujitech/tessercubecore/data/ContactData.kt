package com.sujitech.tessercubecore.data

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcelable
import com.tylersuehr.chips.Chip
import io.requery.*
import io.requery.kotlin.eq
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
interface MessageData : Persistable, Parcelable {
    @get:Key
    @get:Generated
    val dataId: Int
    @get:OneToOne(mappedBy = "messageDataFrom")
    var messageFrom: MessageUserData?
    @get:OneToMany(mappedBy = "messageDataTo")
    val messageTo: MutableList<MessageUserData>
    var content: String
    var rawContent: String
    var composeTime: Date?
    var interpretTime: Date?
    var fromMe: Boolean
    var isDraft: Boolean
    var verifyStatus: VerifyStatus
}

@Entity
interface WalletData : Persistable, Parcelable {
    @get:Key
    @get:Generated
    val dataId: Int
    var address: String
    var passwordId: String
    var mnemonicId: String
    /**
     * ETH balance
     */
    var balance: BigDecimal?

    @get:ForeignKey
    @get:OneToMany
    var walletToken: List<WalletToken>
}

@Entity
interface ERC20Token : Persistable, Parcelable {
    @get:Key
    @get:Generated
    val dataId: Int
    var address: String
    var name: String
    var decimal: Int
    var symbol: String
    var isUserDefine: Boolean
    var deletedAt: Date?

    @get:ForeignKey
    @get:OneToOne
    var walletToken: WalletToken
    @get:ForeignKey
    @get:OneToMany
    var redPacketData: List<RedPacketData>
}

@Entity
interface WalletToken : Persistable, Parcelable {
    @get:Key
    @get:Generated
    val dataId: Int
    @get:ManyToOne
    var wallet: WalletData
    @get:OneToOne
    var token: ERC20Token
    var index: Int
    var tokenBalance: BigDecimal?
}

@Entity
interface RedPacketData : Persistable, Parcelable {
    @get:Key
    @get:Generated
    val dataId: Int
    var aesVersion: Int
    var contractVersion: Int
    var contractAddress: String

    // Split by ;
    var uuids: String
    var isRandom: Boolean
    var failReason: String?
    var createNonce: Int?
    var creationTransactionHash: String?
    var blockCreationTime: Long?
    var duration: Long
    var redPacketId: String?
    var rawPayload: String?
    var encPayload: String?
    var senderAddress: String
    var senderName: String
    var sendTotal: BigDecimal
    var sendMessage: String
    var lastSharedTime: Date?
    var claimAddress: String?
    var claimTransactionHash: String?
    var claimAmount: BigDecimal?
    var refundTransactionHash: String?
    var refundAmount: BigDecimal?
    var status: RedPacketStatus
    var creationFunctionCall: String?
    var claimFunctionCall: String?
    var refundFunctionCall: String?
    var claimNonce: Int?
    var refundNonce: Int?
    var network: RedPacketNetwork
    var tokenType: RedPacketTokenType
    @get:ManyToOne
    var erC20Token: ERC20Token?
    var erc20ApproveTransactionHash: String?
}

val RedPacketData.passwords
    get() = uuids.split(";")

fun RedPacketData.isFromMe(): Boolean {
    return DbContext.data.select(WalletData::class).where(WalletData::address eq this.senderAddress).get().any()
}

enum class RedPacketNetwork {
    Mainnet,
    Rinkeby
}

enum class RedPacketTokenType {
    ETH,
    ERC20
}

enum class RedPacketStatus {
    initial,
    pending,
    fail,
    normal,
    incoming,
    claimPending,
    claimed,
    expired,
    empty,
    refundPending,
    refunded
}