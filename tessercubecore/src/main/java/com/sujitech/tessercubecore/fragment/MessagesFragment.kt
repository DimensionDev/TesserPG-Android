package com.sujitech.tessercubecore.fragment


import android.animation.Animator
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.sujitech.tessercubecore.R
import com.sujitech.tessercubecore.activity.message.ComposeActivity
import com.sujitech.tessercubecore.activity.message.InterpretActivity
import com.sujitech.tessercubecore.activity.wallet.ClaimActivity
import com.sujitech.tessercubecore.common.Settings
import com.sujitech.tessercubecore.common.adapter.AutoAdapter
import com.sujitech.tessercubecore.common.adapter.IItemSelector
import com.sujitech.tessercubecore.common.adapter.getItemsSource
import com.sujitech.tessercubecore.common.adapter.updateItemsSource
import com.sujitech.tessercubecore.common.extension.shareText
import com.sujitech.tessercubecore.common.extension.toActivity
import com.sujitech.tessercubecore.data.DbContext
import com.sujitech.tessercubecore.data.MessageData
import com.sujitech.tessercubecore.data.RedPacketState
import com.sujitech.tessercubecore.widget.MessageCard
import com.sujitech.tessercubecore.widget.RedPacketCard
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_messages.*
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

class MessagesFragment : ViewPagerFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_messages, container, false)
    }

    private val tabs by lazy {
        listOf(
                tabLayout.newTab().apply {
                    this.text = getString(R.string.timeline)
                },
                tabLayout.newTab().apply {
                    this.text = getString(R.string.saved_drafts)
                }
        )
    }

    private val animationCenterX by lazy {
        (add_button.left + add_button.right) / 2
    }
    private val animationCenterY by lazy {
        (add_button.height) / 2
    }
    private val animationRadius by lazy {
        Math.hypot(animationCenterX.toDouble(), animationCenterY.toDouble()).toFloat()
    }

    private val goneAnimationListener by lazy {
        object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                message_actions_container.isVisible = false
                isAnimating = false
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        }
    }
    private val visibleAnimationListener by lazy {
        object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                isAnimating = false
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        }
    }

    private var isAnimating = false
    private fun toggleActionContainer() {
        if (isAnimating) {
            return
        }
        isAnimating = true
        if (message_actions_container.visibility == View.VISIBLE) {
            ViewAnimationUtils.createCircularReveal(message_actions_container, animationCenterX, animationCenterY, animationRadius, 0F).apply {
                addListener(goneAnimationListener)
            }.start()
            add_button.show()
        } else {
            message_actions_container.isVisible = true
            ViewAnimationUtils.createCircularReveal(message_actions_container, animationCenterX, animationCenterY, 0F, animationRadius).apply {
                addListener(visibleAnimationListener)
            }.start()
            add_button.hide()
        }
    }

    private val COMPOSE_REQUEST_CODE: Int = 782

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && event.keyCode == KeyEvent.KEYCODE_BACK && message_actions_container != null) {
            if (message_actions_container.visibility == View.VISIBLE) {
                toggleActionContainer()
                return true
            }

            val searchMenu = toolbar.menu.findItem(R.id.menu_search)
            if (searchMenu.isActionViewExpanded) {
                searchMenu.collapseActionView()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        add_button.setOnClickListener {
            toggleActionContainer()
        }
        compose_button.setOnClickListener {
            toggleActionContainer()
            startActivityForResult(Intent(context, ComposeActivity::class.java), COMPOSE_REQUEST_CODE)
        }
        interpret_button.setOnClickListener {
            toggleActionContainer()
            context.toActivity<InterpretActivity>()
        }
        toolbar.inflateMenu(R.menu.messages_toolbar)
        val searchMenu = toolbar.menu.findItem(R.id.menu_search)
        val searchView = searchMenu.actionView as SearchView
        searchMenu.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                cachedMessages.clear()
                recycler_view.getItemsSource<MessageData>()?.let { cachedMessages.addAll(it) }
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                recycler_view.updateItemsSource(cachedMessages)
                return true
            }

        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                filterMessage(newText)
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
        })
        recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AutoAdapter(object : IItemSelector<MessageData> {
                override fun selectLayout(item: MessageData): Int {
                    return if (item.redPacketData == null) {
                        R.layout.item_message_card
                    } else {
                        R.layout.item_redpacket_card
                    }
                }
            }).apply {
                bindCustom<MessageCard>(R.id.item_message_card_root) { messageCard, messageData, _, _ ->
                    messageCard.messageData = messageData
                }
                bindCustom<RedPacketCard>(R.id.item_red_packet_card_root) { view, data, _, _ ->
                    view.data = data
                    view.setOnLongClickListener {
                        if (data.redPacketData != null &&
                                data.redPacketData?.fromMe == false) {
                            showFromOthersPopupMenu(data, it)
                            return@setOnLongClickListener true
                        }
                        false
                    }
                }
                itemClicked += { sender, args ->
                    if (args.item.redPacketData != null &&
                            args.item.redPacketData?.fromMe == false &&
                            args.item.redPacketData?.state != RedPacketState.claimed
                    ) {
                        context.toActivity<ClaimActivity>(Intent().apply {
                            putExtra("data", args.item)
                        })
                    } else if (args.item.fromMe) {
                        if (args.item.isDraft) {
                            showDraftPopupMenu(args.item, sender as View)
                        } else {
                            showFromMePopupMenu(args.item, sender as View)
                        }
                    } else {
                        showFromOthersPopupMenu(args.item, sender as View)
                    }
                }
                whenEmpty(R.layout.empty_message)
            }
        }
        subscribeMessageList()
        tabs.forEach {
            tabLayout.addTab(it)
        }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateRecyclerViewItemsSource()
            }

        })
    }

    private fun isDraftMode(): Boolean {
        return tabLayout.selectedTabPosition == 1
    }

    val cachedMessages = arrayListOf<MessageData>()

    private fun filterMessage(newText: String?) {
        if (newText.isNullOrEmpty()) {
            recycler_view.updateItemsSource(cachedMessages)
        } else {
            recycler_view.updateItemsSource(cachedMessages.filter {
                it.content.contains(newText) ||
                        it.messageFrom?.name?.contains(newText) == true || it.messageTo.any {
                    it.name?.contains(newText) == true
                }
            })
        }
    }

    private fun showFromMePopupMenu(item: MessageData, view: View) {
        context?.let { context ->
            PopupMenu(context, view).apply {
                this.gravity = Gravity.END
                inflate(R.menu.message_from_me)
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.menu_share_encrypted_message -> {
                            context.shareText(item.rawContent)
                            true
                        }
                        R.id.menu_copy_text -> {
                            context.shareText(item.content)
                            true
                        }
                        R.id.menu_re_compose -> {
                            context.toActivity<ComposeActivity>(Intent().apply {
                                putExtra("mode", ComposeActivity.Mode.ReCompose)
                                putExtra("data", item)
                            })
                            true
                        }
                        R.id.menu_delete -> {
                            DbContext.data.delete(item).blockingGet()
                            true
                        }
                        else -> false
                    }
                }
            }.show()
        }
    }

    private fun showDraftPopupMenu(item: MessageData, view: View) {
        context?.let { context ->
            PopupMenu(context, view).apply {
                this.gravity = Gravity.END
                inflate(R.menu.message_draft)
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.menu_edit -> {
                            context.toActivity<ComposeActivity>(Intent().apply {
                                putExtra("mode", ComposeActivity.Mode.FromDraft)
                                putExtra("data", item)
                            })
                            true
                        }
                        R.id.menu_finish_and_sign -> {
                            context.toActivity<ComposeActivity>(Intent().apply {
                                putExtra("mode", ComposeActivity.Mode.FinishAndSign)
                                putExtra("data", item)
                            })
                            true
                        }
                        R.id.menu_mark_as_finished -> {
                            item.isDraft = false
                            DbContext.data.update(item).blockingGet()
                            true
                        }
                        R.id.menu_delete -> {
                            DbContext.data.delete(item).blockingGet()
                            true
                        }
                        else -> false
                    }
                }
            }.show()
        }
    }

    private fun showFromOthersPopupMenu(item: MessageData, view: View) {
        context?.let { context ->
            PopupMenu(context, view).apply {
                this.gravity = Gravity.END
                inflate(R.menu.message_from_others)
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.menu_copy_clear_text -> {
                            context.shareText(item.content)
                            true
                        }
                        R.id.menu_copy_raw_payload -> {
                            context.shareText(item.rawContent)
                            true
                        }
                        R.id.menu_delete -> {
                            DbContext.data.delete(item).blockingGet()
                            true
                        }
                        else -> false
                    }
                }
            }.show()
        }
    }

    private lateinit var messageSubscribe: Disposable
    private var allMessages = arrayListOf<MessageData>()

    private fun subscribeMessageList() {
        this.messageSubscribe = DbContext.data.select(MessageData::class).get().observableResult().subscribe {
            allMessages.clear()
            allMessages.addAll(it.toList())
            updateRecyclerViewItemsSource()
        }
    }

    private fun updateRecyclerViewItemsSource() {
        recycler_view.updateItemsSource(allMessages.filter { msg -> if (isDraftMode()) msg.isDraft else !msg.isDraft }.reversed())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!messageSubscribe.isDisposed) {
            messageSubscribe.dispose()
        }
    }

    override fun onPageSelected() {
        super.onPageSelected()
        if (!Settings.get("has_message_entry", false)) {
            activity?.let {
                MaterialTapTargetPrompt.Builder(it)
                        .setTarget(add_button)
                        .setPrimaryText(getString(R.string.intro_message_title))
                        .setSecondaryText(getString(R.string.intro_message_desc))
                        .setPromptStateChangeListener { prompt, state ->
                            Settings.set("has_message_entry", true)
                        }
                        .show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == COMPOSE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val message = data.getParcelableExtra<MessageData>("message")
            Snackbar.make(coordinator, getString(R.string.snack_share_message), Snackbar.LENGTH_LONG).apply {
                setAction(android.R.string.ok) {
                    context.shareText(message.rawContent)
                }

                show()
            }
        }
    }

}
