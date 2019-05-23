package com.sujitech.tessercubecore.activity.keypair

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import com.sujitech.tessercubecore.BuildConfig
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.extension.task
import com.sujitech.tessercubecore.common.extension.toUserKeyData
import com.sujitech.tessercubecore.data.DbContext
import kotlinx.android.synthetic.main.activity_create_key.*
import moe.tlaster.kotlinpgp.KotlinPGP
import moe.tlaster.kotlinpgp.data.Algorithm
import moe.tlaster.kotlinpgp.data.Curve
import moe.tlaster.kotlinpgp.data.GenerateKeyData
import moe.tlaster.kotlinpgp.data.KeyData

class CreateKeyActivity : BaseActivity() {

    private val rsaKeyLengthAdapter by lazy {
        ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("1024", "2048", "3072", "4096"))
    }
    private val algorithms by lazy {
        listOf(
                "RSA",
                "ECC(SECP256K1)"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_key)
        create_key_button.setOnClickListener {
            createKeyButtonClicked()
        }
        easy_mode_check_box.setOnCheckedChangeListener { _, isChecked ->
            adv_container.isVisible = !isChecked
        }
        key_algorithm_spinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, algorithms)
        key_algorithm_spinner.setSelection(0)
        key_algorithm_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = key_algorithm_spinner.adapter?.getItem(position)
                if (item is String) {
                    // TODO:
                    (item != algorithms[1]).also {
                        key_length_spinner.isVisible = it
                        title_key_length.isVisible = it
                    }
//                    key_length_spinner.adapter = algorithms[item]
//                    key_length_spinner.setSelection(algorithms[item]!!.count - 2)// TODO
                }
            }

        }
        key_length_spinner.adapter = rsaKeyLengthAdapter
        key_length_spinner.setSelection(2)
        if (BuildConfig.DEBUG) {
            key_algorithm_spinner.setSelection(1)
        }
    }

    private fun createKeyButtonClicked() {
        val name = name_input.text.toString()
        val email = mail_input.text.toString()
        val password = password_input.text.toString()
        val passwordConfirm = password_confirm_input.text.toString()
        var canCreateKey = true

        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mail_input.error = getString(R.string.error_email_format)
            canCreateKey = false
        }

        if (password != passwordConfirm) {
            password_confirm_input.error = getString(R.string.error_password_mismatch)
            password_confirm_input.requestFocus()
            canCreateKey = false
        }

        if (password.length < 8 && !BuildConfig.DEBUG) {
            password_confirm_input.error = getString(R.string.password_desc)
            password_confirm_input.requestFocus()
            canCreateKey = false
        }

        if (!canCreateKey) {
            return
        }
        task {
            val algorithm = when (key_algorithm_spinner.selectedItem) {
                "RSA" -> Algorithm.RSA
                "ECC(SECP256K1)" -> Algorithm.ECDSA
                else -> Algorithm.RSA
            }
            val subKeyAlgorithm = when (key_algorithm_spinner.selectedItem) {
                "RSA" -> Algorithm.RSA
                "ECC(SECP256K1)" -> Algorithm.ELGAMAL
                else -> Algorithm.RSA
            }
            val curve = when(key_algorithm_spinner.selectedItem) {
                "ECC(SECP256K1)" -> Curve.Secp256k1
                else -> null
            }

            kotlin.runCatching {
                val keypair = KotlinPGP.generateKeyPair(GenerateKeyData(
                        name, email, password,
                        masterKey = KeyData(
                                strength = key_length_spinner.selectedItem.toString().toInt(),
                                algorithm = algorithm,
                                curve = curve
                        ),
                        subKey = KeyData(
                                strength = key_length_spinner.selectedItem.toString().toInt(),
                                algorithm = subKeyAlgorithm,
                                curve = curve
                        )
                ))
                val data = KotlinPGP.getSecretKeyRingFromString(keypair.secretKey, password).toUserKeyData(keypair.secretKey, password)
                if (data.hasPassword) {
                    UserPasswordStorage.save(this@CreateKeyActivity, data.uuid, password)
                }
                runOnUiThread {
                    DbContext.data.insert(data).blockingGet()
                    finish()
                }
            }.onFailure {
                it.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@CreateKeyActivity, getString(R.string.error_create_key), Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
}
