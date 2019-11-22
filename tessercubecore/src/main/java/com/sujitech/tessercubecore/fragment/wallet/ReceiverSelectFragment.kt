package com.sujitech.tessercubecore.fragment.wallet

import android.os.Bundle
import android.view.View
import android.widget.CheckedTextView
import androidx.fragment.app.Fragment
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.adapter.getItemsSource
import com.sujitech.tessercubecore.common.adapter.updateItemsSource
import com.sujitech.tessercubecore.data.ContactData
import com.sujitech.tessercubecore.data.DbContext
import kotlinx.android.synthetic.main.fragment_receiver_select.*

class ReceiverSelectFragment : Fragment(R.layout.fragment_receiver_select) {


    var next: (() -> Unit)? = null
    var back: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler_view.adapter = AutoAdapter<ContactDataWrap>(R.layout.item_simple_list_checkable).apply {
            bindCustom<CheckedTextView>(R.id.checked_text_view) { view, item, position, adapter ->
                view.setOnClickListener {
                    view.isChecked = !view.isChecked
                    item.isChecked = view.isChecked
                }
                view.text = item.data.name
            }
        }
        recycler_view.updateItemsSource(DbContext.data.select(ContactData::class).get().map {
            ContactDataWrap(it, false)
        }.toList())


        toolbar.setNavigationOnClickListener {
            back?.invoke()
        }
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                android.R.id.home -> {
                    back?.invoke()
                    true
                }

                R.id.menu_done -> {
                    next?.invoke()
                    true
                }

                else -> false
            }
        }
    }

    fun getSelectedReceiver() : List<ContactData> {
        return recycler_view.getItemsSource<ContactDataWrap>()?.let {
            it.filter { it.isChecked }.map { it.data }
        } ?: emptyList()
    }

}

data class ContactDataWrap(
        val data: ContactData,
        var isChecked: Boolean
)