package com.sujitech.tessercubecore


import androidx.test.ext.junit.runners.AndroidJUnit4
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyPairGenerator
import java.security.Security
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
        val g = KeyPairGenerator.getInstance("EC")
        val kpgparams = ECGenParameterSpec("secp256k1")
        g.initialize(kpgparams)
        val pair = g.generateKeyPair()
        println((pair.private as BCECPrivateKey).s.toString(16))
        (pair.public as ECPublicKey).w
////        val a = getPublicKeyDetails(pair.public as BCECPublicKey)
//        val field = pair.public.javaClass.superclass.getDeclaredField("key")
//        field.isAccessible = true
//        val key = field.get(pair.public) as ByteArray
//        println(key.toHexString())
//        println(pair.public as ECPublicKey)
//        println(pair.public.encoded.toHexString())
    }
}
