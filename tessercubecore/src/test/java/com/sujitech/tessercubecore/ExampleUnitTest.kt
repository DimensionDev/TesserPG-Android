package com.sujitech.tessercubecore
//
//import com.sujitech.tessercubecore.common.extension.toHexString
//import moe.tlaster.kotlinpgp.KotlinPGP
//import moe.tlaster.kotlinpgp.data.Algorithm
//import moe.tlaster.kotlinpgp.data.Curve
//import moe.tlaster.kotlinpgp.data.GenerateKeyData
//import moe.tlaster.kotlinpgp.data.KeyData
//import org.bitcoinj.crypto.HDUtils
//import org.bitcoinj.crypto.MnemonicCode
//import org.bitcoinj.wallet.DeterministicKeyChain
//import org.bitcoinj.wallet.DeterministicSeed
//import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
//import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
//import org.bouncycastle.jce.ECNamedCurveTable
//import org.bouncycastle.jce.provider.BouncyCastleProvider
//import org.bouncycastle.jce.spec.ECPrivateKeySpec
//import org.junit.Test
//import org.web3j.crypto.MnemonicUtils
//import java.math.BigInteger
//import java.security.*
//import java.security.interfaces.ECPrivateKey
//import java.security.spec.ECGenParameterSpec
//import java.security.spec.PKCS8EncodedKeySpec
//
//
//fun getPrivateKeyFromECBigIntAndCurve(s: BigInteger, curveName: String): PrivateKey? {
//    val ecParameterSpec = ECNamedCurveTable.getParameterSpec(curveName)
//    val privateKeySpec = ECPrivateKeySpec(s, ecParameterSpec)
//    val keyFactory = KeyFactory.getInstance("EC")
//    return keyFactory.generatePrivate(privateKeySpec)
//}
//
///**
// * Example local unit test, which will execute on the development machine (host).
// *
// * @see [Testing documentation](http://d.android.com/tools/testing)
// */
//class ExampleUnitTest {
//    fun compressPubKey(pubKey: BigInteger): String {
//        val pubKeyYPrefix = if (pubKey.testBit(0)) "03" else "02"
//        val pubKeyHex = pubKey.toString(16)
//        val pubKeyX = pubKeyHex.substring(0, 64)
//        return pubKeyYPrefix + pubKeyX
//    }
//
//    @Test
//    fun test5() {
//        val kp = KotlinPGP.generateKeyPair(GenerateKeyData(
//                name = "test",
//                email = "test@test.com",
//                password = "password",
//                masterKey = KeyData(
//                        algorithm = Algorithm.ECDSA,
//                        curve = Curve.Secp256k1
//                ),
//                subKey = KeyData(
//                        algorithm = Algorithm.ECDSA,
//                        curve = Curve.Secp256k1
//                )
//        ))
//        val sec = KotlinPGP.getSecretKeyRingFromString(kp.secretKey, "password")
//        println(sec.secretKey.encoded.size)
//        println(sec.encoded.size)
//        println(MnemonicCode.INSTANCE.toMnemonic(sec.secretKey.encoded))
//        println(MnemonicCode.INSTANCE.toMnemonic(sec.encoded))
//    }
//
//    @Test
//    fun test3() {
//        val initialEntropy = ByteArray(16)
//        SecureRandomUtils.secureRandom().nextBytes(initialEntropy)
//
////        val mnemonic = MnemonicUtils.generateMnemonic(initialEntropy)
////        val seed = MnemonicUtils.generateSeed(mnemonic, "password")
////        val pair = Bip32ECKeyPair.generateKeyPair(seed)
//
////        assert(pair != null)
////        Keys.createEcKeyPair()
////
////        val initialEntropy = ByteArray(16)
////        SecureRandomUtils.secureRandom().nextBytes(initialEntropy)
////        val mnemonic = MnemonicUtils.generateMnemonic(initialEntropy)
////        val seed = MnemonicUtils.generateSeed(mnemonic, "password")
////        val pair = ECKeyPair.create(sha256(seed))
////        val privKey = getPrivateKeyFromECBigIntAndCurve(pair.privateKey, "secp256k1")
////        val pubKey = pair.publicKey
////        val msg = "Message for signing"
////
////        val ecdsaSign = Signature.getInstance("SHA256withECDSA", BouncyCastleProvider.PROVIDER_NAME)
////        ecdsaSign.initSign(privKey)
////        ecdsaSign.update(msg.toByteArray())
////        val signature = ecdsaSign.sign()
////        val ecdsaVerify = Signature.getInstance("SHA256withECDSA", BouncyCastleProvider.PROVIDER_NAME)
////        ecdsaVerify.initVerify(privKey)
////        ecdsaVerify.update(msg.toByteArray())
////        val result = ecdsaVerify.verify(signature)
////        assert(result)
//
//    }
//
//    @Test
//    fun test1() {
//        val initialEntropy = ByteArray(16)
//        SecureRandomUtils.secureRandom().nextBytes(initialEntropy)
//        val mnemonic = MnemonicUtils.generateMnemonic(initialEntropy)
//        val seed = DeterministicSeed(mnemonic, null, "", System.currentTimeMillis() / 1000)
//        val chain = DeterministicKeyChain.builder().seed(seed).build()
//        val keyPath = HDUtils.parsePath("M/44H/60H/0H/0/0")
//
//        val key = chain.getKeyByPath(keyPath, true)
//        val privKey = key.privKey.toString(16)
//        val pubKey = key.publicKeyAsHex
//        assert(privKey != null)
//
////        val wordsList = "one misery space industry hen mistake typical prison plunge yellow disagree arm"
////        val deterministicSeed = DeterministicSeed(wordsList, null, "", 0L)
////        val deterministicKeyChain = DeterministicKeyChain.builder().seed(deterministicSeed).build()
////        val privKey = deterministicKeyChain.getKeyByPath(HDUtils.parsePath("44H / 1H / 0H / 0 / 2"), true).privKey
////        val ecKey = ECKey.fromPrivate(privKey)
////        assert(ecKey != null)
////        val address = ecKey.toAddress(params)
////        System.out.println(address.toBase58())
//
//
////        val initialEntropy = ByteArray(16)
////        SecureRandomUtils.secureRandom().nextBytes(initialEntropy)
////        val mnemonic = MnemonicUtils.generateMnemonic(initialEntropy)
////        val seed = MnemonicUtils.generateSeed(mnemonic, "password")
////        val pair = ECKeyPair.create(sha256(seed))
////        val privKey = pair.privateKey
////        val pubKey = pair.publicKey
////        val a = ECKeyPair.create(seed)
////
////        System.out.println("mnemonic: $mnemonic")
////        System.out.println("Private key: " + privKey.toString(16))
////        System.out.println("Public key: " + pubKey.toString(16))
////        System.out.println("Public key (compressed): " + compressPubKey(pubKey))
////
////        val msg = "Message for signing"
//////        val msgHash = Hash.sha3(msg.toByteArray())
////        val signature = Sign.signMessage(msg.toByteArray(), pair)
//////        val signature = Sign.signMessage(msgHash, pair, false)
////        println("Msg: $msg")
//////        System.out.println("Msg hash: " + Hex.toHexString(msgHash))
////        System.out.printf("Signature: [v = %d, r = %s, s = %s]\n",
////                signature.v - 27,
////                Hex.toHexString(signature.r),
////                Hex.toHexString(signature.s))
////
////        println()
////
////        val pubKeyRecovered = Sign.signedMessageToKey(msg.toByteArray(), signature)
////        println("Recovered public key: " + pubKeyRecovered.toString(16))
////
////        val validSig = pubKey.equals(pubKeyRecovered)
////        println("Signature valid? $validSig")
//    }
//
//    @Test
//    fun test4() {
////        val pair = Keys.createEcKeyPair()
////        println(pair.privateKey.toString(16))
////        println(pair.publicKey.toString(16))
//    }
//
//    @Test
//    fun test2() {
//        Security.addProvider(BouncyCastleProvider())
//        val g = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
//        val kpgparams = ECGenParameterSpec("secp256k1")
//        g.initialize(kpgparams)
//        val pair = g.generateKeyPair()
//        val code = MnemonicCode.INSTANCE.toMnemonic((pair.private as BCECPrivateKey).encoded)
//        println(code)
//        val bytes = MnemonicCode.INSTANCE.toEntropy(code)
//        val pkcs8EncodedKeySpec = PKCS8EncodedKeySpec(bytes)
//        val privateKey2 = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME).generatePrivate(pkcs8EncodedKeySpec)
//        assert((privateKey2 as BCECPrivateKey).s == (pair.private as BCECPrivateKey).s)
//
////        val a = (pair.private as BCECPrivateKey).s.toByteArray()
////        val b = MnemonicUtils.generateMnemonic(a)
////        val c = MnemonicUtils.generateSeed(b, "password")
//        println((pair.private as ECPrivateKey).s.toString(16))
//        println((pair.public as BCECPublicKey).q.getEncoded(false).toHexString())
//        val msg = "text ecdsa with sha256"
//        val ecdsaSign = Signature.getInstance("SHA256withECDSA", BouncyCastleProvider.PROVIDER_NAME)
//        ecdsaSign.initSign(pair.private)
//        ecdsaSign.update(msg.toByteArray())
//        val signature = ecdsaSign.sign()
//        println(signature.toHexString())
//        val ecdsaVerify = Signature.getInstance("SHA256withECDSA", BouncyCastleProvider.PROVIDER_NAME)
//        ecdsaVerify.initVerify(pair.public)
//        ecdsaVerify.update(msg.toByteArray())
//        val result = ecdsaVerify.verify(signature)
//
//        assert(result)
//    }
//
//}
//
//fun sha256(input: ByteArray): ByteArray {
//    try {
//        val digest = MessageDigest.getInstance("SHA-256")
//        return digest.digest(input)
//    } catch (e: NoSuchAlgorithmException) {
//        throw RuntimeException("Couldn't find a SHA-256 provider", e)
//    }
//}
//
//internal object SecureRandomUtils {
//
//    private val SECURE_RANDOM: SecureRandom
//
//    // Taken from BitcoinJ implementation
//    // https://github.com/bitcoinj/bitcoinj/blob/3cb1f6c6c589f84fe6e1fb56bf26d94cccc85429/core/src/main/java/org/bitcoinj/core/Utils.java#L573
//    private var isAndroid = -1
//
//    val isAndroidRuntime: Boolean
//        get() {
//            if (isAndroid == -1) {
//                val runtime = System.getProperty("java.runtime.name")
//                isAndroid = if (runtime != null && runtime == "Android Runtime") 1 else 0
//            }
//            return isAndroid == 1
//        }
//
//    init {
//        if (isAndroidRuntime) {
////            LinuxSecureRandom()
//        }
//        SECURE_RANDOM = SecureRandom()
//    }
//
//    fun secureRandom(): SecureRandom {
//        return SECURE_RANDOM
//    }
//}
