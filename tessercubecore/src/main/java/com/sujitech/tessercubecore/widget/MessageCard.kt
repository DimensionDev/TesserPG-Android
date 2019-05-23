package com.sujitech.tessercubecore.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.adapter.updateItemsSource
import com.sujitech.tessercubecore.common.extension.dp
import com.sujitech.tessercubecore.common.prettyTime
import com.sujitech.tessercubecore.common.toColor
import com.sujitech.tessercubecore.data.MessageData
import com.sujitech.tessercubecore.data.MessageUserData
import kotlinx.android.synthetic.main.item_message_contact.view.*
import kotlinx.android.synthetic.main.widget_message_card.view.*
import kotlin.math.min

class MessageCard : CardView {

    private val messageToAdapter by lazy {
        AutoAdapter<MessageUserData>(R.layout.item_message_contact).apply {
            bindText(R.id.item_message_contact_title) {
                it.name ?: ""
            }
            bindText(R.id.item_message_contact_desc) {
                if (it.email != null) {
                    "(${it.email})"
                } else {
                    ""
                }
            }
            bindText(R.id.item_message_contact_hash) {
                it.keyId?.toString(16)?.takeLast(8) ?: ""
            }
            bindFooter {
                if (it is TextView) {
                    it.setOnClickListener {
                        footerEnabled = false
                        widget_message_to_list.updateItemsSource(messageData?.messageTo)
                    }
                    val data = messageData
                    if (data != null) {
                        val moreCount = data.messageTo.count() - 2
                        if (moreCount > 0) {
                            it.text = context.getString(R.string.message_email_expand, contentLines)
                        }
                    }
                }
            }
            withFooter(TextView(context).apply {
                text = ""
            })
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_message_card, this)
        cardElevation = 2.dp.toFloat()
        radius = 8.dp.toFloat()
        widget_message_to_list.apply {
            adapter = messageToAdapter
        }
        widget_message_show_all_lines.setOnClickListener {
            if (widget_message_content.maxLines == 2) {
                widget_message_content.maxLines = Int.MAX_VALUE
                widget_message_show_all_lines.text = context.getString(R.string.message_hide_full_content, contentLines)
            } else {
                widget_message_content.maxLines = 2
                widget_message_show_all_lines.text = context.getString(R.string.message_show_full_content, contentLines)
            }
        }
    }

    var messageData: MessageData? = null
        set(value) {
            field = value
            updateView(value)
        }

    private var contentLines: Int = 0

    private fun updateView(value: MessageData?) {
        item_message_contact_title.text = value?.messageFrom?.name ?: context.getString(R.string.message_unknown_sender_title)
        item_message_contact_desc.text = if (value?.messageFrom?.email != null) {
            "(${value.messageFrom?.email})"
        } else  {
            ""
        }
        item_message_contact_hash.text = if (value?.isDraft == true) {
            context.getString(R.string.draft)
        } else {
            value?.messageFrom?.keyId?.toString(16)?.takeLast(8) ?: ""
        }
        val textColor = if (value?.isDraft == true) {
            Color.BLACK
        } else {
            value?.verifyStatus?.toColor() ?: Color.BLACK
        }
        item_message_contact_hash.setTextColor(textColor)//TODO: get primary text color
        widget_message_content.maxLines = Int.MAX_VALUE
        widget_message_content.text = value?.content
        widget_message_content.post {//TODO: might have issue...
            contentLines = widget_message_content.lineCount
            widget_message_content.maxLines = 2
            if (contentLines > 2) {
                widget_message_show_all_lines.apply {
                    isVisible = true
                    text = context.getString(R.string.message_show_full_content, contentLines)
                }
            } else {
                widget_message_show_all_lines.isVisible = false
            }
        }
        if (value == null || value.isDraft || value.fromMe) {
            widget_message_interpret_time.isVisible = false
        } else {
            widget_message_interpret_time.isVisible = true
            widget_message_interpret_time.text = context.getString(R.string.message_interpreted_time, prettyTime.format(value.interpretTime))
        }
        widget_message_compose_time.text = context.getString(R.string.message_composed_time, prettyTime.format(value?.composeTime))
        val maxList = value?.messageTo?.subList(0, min(value.messageTo.count(), 2))
        messageToAdapter.footerEnabled = maxList != null && maxList.count() != value.messageTo.count()
        widget_message_to_list.updateItemsSource(maxList)
    }

}