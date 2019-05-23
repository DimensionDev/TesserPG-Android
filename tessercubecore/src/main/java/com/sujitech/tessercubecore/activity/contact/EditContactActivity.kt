package com.sujitech.tessercubecore.activity.contact

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.BaseActivity
import com.sujitech.tessercubecore.data.ContactDataEntity
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.TrustLevel
import kotlinx.android.synthetic.main.activity_edit_contact.*




class EditContactActivity : BaseActivity() {

    private val data: ContactDataEntity by lazy {
        intent.getParcelableExtra<ContactDataEntity>("data")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_contact)
        update_from_server_button.setOnClickListener {

        }
        delete_contact_button.setOnClickListener {
            DbContext.data.delete(data).blockingGet()
            finish()
        }
        name_input.setText(data.name)
        mail_input.setText(data.email)
        trust_level_spinner.adapter = ArrayAdapter<TrustLevel>(this, android.R.layout.simple_spinner_dropdown_item, TrustLevel.values())
        trust_level_spinner.post {
            trust_level_spinner.setSelection(TrustLevel.values().indexOf(data.trustLevel))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null && item.itemId == R.id.menu_done) {
            data.name = name_input.text.toString()
            data.email = mail_input.text.toString()
            data.trustLevel = trust_level_spinner.selectedItem as TrustLevel
            DbContext.data.update(data).blockingGet()
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.edit_contact_toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

}
