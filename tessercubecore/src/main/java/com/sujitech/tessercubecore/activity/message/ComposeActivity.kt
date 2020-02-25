package com.sujitech.tessercubecore.activity.message

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.common.UserPasswordStorage
import com.sujitech.tessercubecore.common.extension.*
import com.sujitech.tessercubecore.data.*
import kotlinx.android.synthetic.main.activity_compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.tlaster.kotlinpgp.KotlinPGP
import moe.tlaster.kotlinpgp.data.EncryptParameter
import moe.tlaster.kotlinpgp.data.PublicKeyData
import java.util.*

class ComposeActivity : BaseActivity() {
    enum class Mode {
        ReCompose,
        FromDraft,
        FinishAndSign,
        Normal,
        ProcessText,
        ToContact
    }

    private val contacts by lazy {
        DbContext.data.select(ContactData::class).get().toList()
    }
    private val userKeys by lazy {
        DbContext.data.select(UserKeyData::class).get().toList().let {
            ArrayList(it).apply {
                add(0, null)
            }
        }
    }

    private lateinit var cachedData: MessageData
    private lateinit var composeMode: Mode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose)
        compose_from_spinner.adapter = object : ArrayAdapter<UserKeyData>(this, R.layout.item_message_contact, userKeys) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView
                        ?: layoutInflater.inflate(R.layout.item_message_contact, parent, false)
                view.setPadding(8.dp, 8.dp, 8.dp, 8.dp)
                val item = getItem(position)
                val contactData = item?.contactData
                if (contactData != null) {
                    view.findViewById<TextView>(R.id.item_message_contact_title).text = contactData.name
                    view.findViewById<TextView>(R.id.item_message_contact_desc).text = contactData.email
                    view.findViewById<TextView>(R.id.item_message_contact_hash).text = contactData.keyData.firstOrNull()?.fingerPrint?.toUpperCase()?.takeLast(8)
                            ?: ""
                } else {
                    view.findViewById<TextView>(R.id.item_message_contact_title).text = getString(R.string.compose_without_signature)
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return getView(position, convertView, parent)
            }
        }
        compose_to_input.setFilterableChipList(contacts)
        compose_to_input.post {
            compose_to_input.requestFocus()
        }
        composeMode = if (intent.hasExtra("mode")) {
            intent.getSerializableExtra("mode") as Mode
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && intent?.action == Intent.ACTION_PROCESS_TEXT && intent.hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
            compose_content.setText(intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT))
            Mode.ProcessText
        } else {
            Mode.Normal
        }
        if (intent.hasExtra("data")) {
            cachedData = DbContext.data.findByKey(MessageData::class, intent.getParcelableExtra<MessageData>("data").dataId).blockingGet()
            val selectedTo = cachedData.messageTo.map { messageUserData ->
                contacts.firstOrNull { contact ->
                    contact.keyData.any { key ->
                        key.keyId == messageUserData.keyId
                    }
                }
            }
            if (selectedTo.any()) {
                compose_to_input.setSelectedChipList(selectedTo)
            }
            val selectedFrom = userKeys.indexOfFirst {
                cachedData.messageFrom?.keyId == it?.contactData?.keyId
            }
            if (selectedFrom != -1) {
                compose_from_spinner.setSelection(selectedFrom, false)
            }
            compose_content.setText(cachedData.content)
        }
        if (composeMode == Mode.FinishAndSign) {
            updateMessage()
        } else if (composeMode == Mode.ToContact) {
            val selectedTo = intent.getParcelableExtra<ContactData>("contact")
            compose_to_input.setSelectedChipList(listOf(selectedTo))
        }
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                R.id.menu_done -> {
                    when (composeMode) {
                        Mode.FromDraft -> updateMessage()
                        Mode.ReCompose, Mode.Normal, Mode.ToContact -> compose()
                        Mode.ProcessText -> returnProcessResult()
                        else -> return super.onOptionsItemSelected(item)
                    }
                    return true
                }
                android.R.id.home -> {
                    return if (compose_to_input.selectedChips.any() ||
                            (compose_from_spinner.selectedItem as? UserKeyData)?.contactData != null ||
                            compose_content.text.isNotEmpty()) {
                        asyncScope.launch {
                            val saveDraft = alert(getString(R.string.compose_save_draft))
                            if (saveDraft) {
                                saveMessageDraft()
                            } else {
                                finish()
                            }
                        }
                        true
                    } else {
                        super.onOptionsItemSelected(item)
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun returnProcessResult() {
        task {
            val messageData = MessageDataEntity()
            if (messageData.composeMessageData()) {
                runOnUiThread {
                    DbContext.data.insert(messageData).blockingGet()
                }
                val readonly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)
                if (!readonly) {
                    val intent = Intent().apply {
                        putExtra(Intent.EXTRA_PROCESS_TEXT, messageData.rawContent)
                    }
                    setResult(Activity.RESULT_OK, intent)
                }
                finish()
            }
        }
    }

    private fun saveMessageDraft() {
        val sendTo = compose_to_input.selectedChips.map { it as ContactData }
        val sendFrom = compose_from_spinner.selectedItem as? UserKeyData
        val sendFromContactData = sendFrom?.contactData
        val message = compose_content.text.toString()
        val messageData = (if (composeMode == Mode.FromDraft) cachedData else MessageDataEntity()).apply {
            applyMessageData(message, "", sendFromContactData, sendTo)
            fromMe = true
            isDraft = true
        }
        runOnUiThread {
            if (composeMode == Mode.FromDraft) {
                DbContext.data.update(messageData).blockingGet()
            } else {
                DbContext.data.insert(messageData).blockingGet()
            }
        }
        finish()
    }

    private fun updateMessage() {
        task {
            if (cachedData.composeMessageData()) {
                runOnUiThread {
                    DbContext.data.update(cachedData).blockingGet()
                }
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra("message", cachedData)
                })
                finish()
            }
        }
    }


    private suspend fun encryptData(sendFromContactData: ContactData?, message: String, sendTo: List<ContactData>, sendFrom: UserKeyData?): EncryptParameter? {
        var encryptData: EncryptParameter? = null
        if (sendFromContactData == null || sendFrom == null) {
            // without signing
            val canContinue = alert(getString(R.string.warning_compose_no_signature))
            if (canContinue) {
                encryptData = EncryptParameter(
                        message = message,
                        publicKey = sendTo.map { PublicKeyData(
                                key = it.pubKeyContent
                        ) }
                )
            }
        } else {
            val result = withContext(Dispatchers.Main) {
                biometricAuthentication("Authentication Required", "Require authentication to sign your message")
            }
            if (result) {
                val password = if (sendFrom.hasPassword) {
                    UserPasswordStorage.get(this@ComposeActivity, sendFrom.uuid) ?: ""
                } else {
                    ""
                }
                encryptData = EncryptParameter(
                        message = message,
                        publicKey = sendTo.map { PublicKeyData(it.pubKeyContent) },
                        enableSignature = true,
                        privateKey = sendFrom.priKey,
                        password = password
                )
            }
        }
        return encryptData
    }

    private suspend fun MessageData.composeMessageData(): Boolean {
        val sendTo = compose_to_input.selectedChips.map { it as ContactData }
        val sendFrom = compose_from_spinner.selectedItem as? UserKeyData
        val sendFromContactData = sendFrom?.contactData
        val message = compose_content.text.toString()
        if (!sendTo.any() && sendFromContactData == null) {
            toast(getString(R.string.error_compose_no_receiver))
            compose_to_input.requestFocus()
            return false
        }
        val encryptData = encryptData(sendFromContactData, message, sendTo, sendFrom)
        if (encryptData != null) {
            val result = KotlinPGP.encrypt(encryptData)
            applyMessageData(message, result, sendFromContactData, sendTo)
            return true
        }
        return false
    }

    private fun compose() {
        task {
            val messageData = MessageDataEntity()
            if (messageData.composeMessageData()) {
                runOnUiThread {
                    DbContext.data.insert(messageData).blockingGet()
                }
                setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra("message", messageData)
                })
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.compose_toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }
}

