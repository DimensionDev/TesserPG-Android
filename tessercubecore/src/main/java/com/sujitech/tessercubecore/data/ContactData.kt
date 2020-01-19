package com.sujitech.tessercubecore.data

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcelable
import com.sujitech.tessercubecore.common.extension.format
import com.sujitech.tessercubecore.common.wallet.currentEthNetworkType
import com.tylersuehr.chips.Chip
import io.requery.*
import io.requery.kotlin.eq
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.tlaster.kotlinpgp.data.VerifyStatus
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.util.*
import kotlin.math.pow

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
     * ETH Mainnet balance
     */
    var balance: BigDecimal?

    /**
     * ETH Rinkeby balance
     */
    var rinkebyBalance: BigDecimal?

    var ropstenBalance: BigDecimal?

    @get:OneToMany
    val walletToken: MutableList<WalletToken>
}

fun WalletData.updateBalance(network: RedPacketNetwork, bigDecimal: BigDecimal) {
    when (network) {
        RedPacketNetwork.Mainnet -> balance = bigDecimal
        RedPacketNetwork.Rinkeby -> rinkebyBalance = bigDecimal
        RedPacketNetwork.Ropsten -> ropstenBalance = bigDecimal
    }
}

val WalletData.currentBalance
    get() = when (currentEthNetworkType) {
        RedPacketNetwork.Mainnet -> this.balance
        RedPacketNetwork.Rinkeby -> this.rinkebyBalance
        RedPacketNetwork.Ropsten -> this.ropstenBalance
    }

@Entity
interface ERC20Token : Persistable, Parcelable {
    @get:Key
    @get:Generated
    val dataId: Int
    var address: String
    var name: String
    var decimals: Int
    var symbol: String
    var isUserDefine: Boolean
    var deletedAt: Date?
    var network: RedPacketNetwork

    @get:OneToMany
    val walletToken: MutableList<WalletToken>
    @get:OneToMany
    val redPacketData: MutableList<RedPacketData>
}

@Entity
interface WalletToken : Persistable, Parcelable {
    @get:Key
    @get:Generated
    val dataId: Int
    @get:ForeignKey
    @get:ManyToOne(cascade = [CascadeAction.NONE])
    var wallet: WalletData
    @get:ForeignKey
    @get:ManyToOne(cascade = [CascadeAction.NONE])
    var token: ERC20Token
    var orderIndex: Int
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
    var password: String
    var shares: Int
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
    var erc20ApproveNonce: Int?
    var erc20ApproveFunctionCall: String?
    var erc20ApproveResult: BigDecimal?
}

//val RedPacketData.passwords
//    get() = uuids.split(";")

val RedPacketData.actualValue
    get() = if (erC20Token == null) { //ETH
        Convert.fromWei(sendTotal.toString(), Convert.Unit.ETHER)
    } else {
        sendTotal.divide(10.0.pow(erC20Token!!.decimals).toBigDecimal())
    }.format(4)

fun BigDecimal.formatToken(isERC20: Boolean, decimals: Int? = null): BigDecimal {
    return if (isERC20) {
        this.divide(10.0.pow(decimals!!).toBigDecimal())
    } else {
        Convert.fromWei(this.toString(), Convert.Unit.ETHER)
    }
}

val RedPacketData.unit
    get() = if (erC20Token == null) { //ETH
        "ETH"
    } else {
        erC20Token!!.symbol
    }

fun RedPacketData.isFromMe(): Boolean {
    return DbContext.data.select(WalletData::class).where(WalletData::address eq this.senderAddress).get().any()
}

enum class RedPacketNetwork {
    Mainnet,
    Rinkeby,
    Ropsten
}

@Serializable
enum class RedPacketTokenType {
    @SerialName("eth")
    ETH,
    @SerialName("erc20")
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