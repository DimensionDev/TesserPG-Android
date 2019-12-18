package com.sujitech.tessercubecore.common.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.sujitech.tessercubecore.common.collection.CollectionChangedEventArg
import com.sujitech.tessercubecore.common.collection.CollectionChangedType
import com.sujitech.tessercubecore.common.collection.INotifyCollectionChanged
import com.sujitech.tessercubecore.common.collection.ObservableCollection
import com.sujitech.tessercubecore.common.extension.load


fun <T> RecyclerView.getItemsSource(): ObservableCollection<T>? {
    val adapterCopy = this.adapter
    if (adapterCopy is AutoAdapter<*>) {
        return adapterCopy.items as ObservableCollection<T>
    }
    return null
}

fun <T> RecyclerView.updateItemsSource(newItems: Collection<T>?) {
    val itemsSource = getItemsSource<T>()
    if (itemsSource != null) {
        itemsSource.clear()
        if (newItems != null) {
            itemsSource.addAll(newItems)
        }
    }
}

class AutoAdapter<T>(@LayoutRes val layout: Int = android.R.layout.simple_list_item_1, val itemPadding: Int = 0)
    : androidx.recyclerview.widget.RecyclerView.Adapter<AutoAdapter.AutoViewHolder>(), Observer<CollectionChangedEventArg> {
    class AutoViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView)
    private enum class ViewType {
        Item,
        EmptyView,
        Header,
        Footer;

        companion object {
            private val values = values()
            fun getByValue(value: Int) = values.firstOrNull { it.ordinal == value }
        }
    }

    data class ItemClickEventArg<T>(
            val item: T,
            val view: View
    )

    data class ActionData<T>(
            @IdRes val id: Int,
            val action: (View, T, position: Int, AutoAdapter<T>) -> Unit
    )

    var items: List<T> = ObservableCollection<T>().apply {
        collectionChanged.observeForever(this@AutoAdapter)
    }
        set(value) {
            field = value

            val current = field
            if (current is INotifyCollectionChanged) {
                current.collectionChanged.removeObserver(this)
            }
            field = value
            if (value is INotifyCollectionChanged) {
                value.collectionChanged.observeForever(this)
            }

            notifyDataSetChanged()
        }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        items.let {
            it as? INotifyCollectionChanged
        }?.let {
            it.collectionChanged.removeObserver(this)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (items.count() == 0 && emptyView != 0) {
            return ViewType.EmptyView.ordinal
        }

        if (hasHeader && position == 0) {
            return ViewType.Header.ordinal
        }

        if (hasFooter) {
            var requirePosition = items.count()
            if (hasHeader) {
                requirePosition += 1
            }
            if (position == requirePosition) {
                return ViewType.Footer.ordinal
            }
        }

        return ViewType.Item.ordinal
    }

    private val actions: ArrayList<ActionData<T>> = ArrayList()

    var itemClicked: MutableLiveData<ItemClickEventArg<T>> = MutableLiveData()
    var itemLongPressed: MutableLiveData<ItemClickEventArg<T>> = MutableLiveData()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AutoViewHolder {
        val type = ViewType.getByValue(viewType)
        when (type) {
            ViewType.Item -> return AutoViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false).apply {
                if (itemPadding != 0) {
                    setPadding(itemPadding, itemPadding, itemPadding, itemPadding)
                }
            })
            ViewType.EmptyView -> {
                if (emptyView != 0) {
                    return AutoViewHolder(LayoutInflater.from(parent.context).inflate(emptyView, parent, false))
                }
            }
            ViewType.Header -> {
                if (headerViewRes != 0) {
                    return AutoViewHolder(LayoutInflater.from(parent.context).inflate(headerViewRes, parent, false))
                }
                val view = headerView
                if (view != null) {
                    return AutoViewHolder(view)
                }
            }
            ViewType.Footer -> {
                if (footerViewRes != 0) {
                    return AutoViewHolder(LayoutInflater.from(parent.context).inflate(footerViewRes, parent, false))
                }
                val view = footerView
                if (view != null) {
                    return AutoViewHolder(view)
                }
            }
        }
        return AutoViewHolder(View(parent.context))
    }

    override fun getItemCount(): Int {
        var count = items.count()
        if (count == 0) {
            count += if (emptyView == 0) {
                0
            } else {
                1
            }
        } else {
            count += if (hasHeader) {
                1
            } else {
                0
            }
            count += if (hasFooter) {
                1
            } else {
                0
            }
        }
        return count
    }

    override fun onBindViewHolder(viewHolder: AutoViewHolder, position: Int) {
        var actualPosition = position
        if (hasHeader) {
            actualPosition -= 1
        }
        if (hasHeader && actualPosition == -1) {
            onBindHeader?.invoke(viewHolder.itemView)
        }

        if (hasFooter && actualPosition == items.count()) {
            onBindFooter?.invoke(viewHolder.itemView)
        }

        val item = items.getOrNull(actualPosition)
        if (item != null) {
            viewHolder.itemView.setOnClickListener {
                itemClicked.value = ItemClickEventArg(item, it)
            }
            viewHolder.itemView.setOnLongClickListener {
                itemLongPressed.value = ItemClickEventArg(item, it)
                itemLongPressed.hasObservers()
            }
            actions.forEach {
                it.action.invoke(viewHolder.itemView.findViewById(it.id), item, actualPosition, this)
            }
        }
    }

    var footerEnabled = true
    var headerEnabled = true

    private val hasHeader
        get() = (headerView != null || headerViewRes != 0) && headerEnabled
    private val hasFooter
        get() = (footerView != null || footerViewRes != 0) && footerEnabled

    private var emptyView: Int = 0
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun whenEmpty(@LayoutRes id: Int) {
        emptyView = id
    }

    private var headerViewRes: Int = 0

    fun withHeader(@LayoutRes id: Int) {
        headerViewRes = id
    }

    private var headerView: View? = null

    fun withHeader(view: View) {
        headerView = view
    }

    private var footerViewRes: Int = 0

    fun withFooter(@LayoutRes id: Int) {
        footerViewRes = id
    }

    private var footerView: View? = null

    fun withFooter(view: View) {
        footerView = view
    }

    private var onBindHeader: ((View) -> Unit)? = null

    fun bindHeader(block: (view: View) -> Unit) {
        onBindHeader = block
    }

    private var onBindFooter: ((View) -> Unit)? = null

    fun bindFooter(block: (view: View) -> Unit) {
        onBindFooter = block
    }

    fun bindImage(@IdRes id: Int, value: (T) -> String) {
        actions.add(ActionData(id) { view, item, _, _ ->
            if (view is ImageView) {
                view.load(value.invoke(item))
            }
        })
    }

    //
//    fun bindCustom(@IdRes id: Int, action: (View, T, position: Int, AutoAdapter<T>) -> Unit) {
//        actions.add(ActionData(id, action))
//    }
//
    fun <K : View> bindCustom(@IdRes id: Int, action: (K, T, position: Int, AutoAdapter<T>) -> Unit) {
        actions.add(ActionData(id) { view, t, position, autoAdapter ->
            if (view as? K != null) {
                action.invoke(view, t, position, autoAdapter)
            }
        })
    }

    fun bindText(@IdRes id: Int, value: (T) -> String) {
        actions.add(ActionData(id) { view, item, _, _ ->
            if (view is TextView) {
                view.text = value.invoke(item)
            }
        })
    }

    override fun onChanged(args: CollectionChangedEventArg) {
        when (args.type) {
            CollectionChangedType.Add -> notifyItemRangeInserted(args.index, args.count)
            CollectionChangedType.Remove -> notifyItemRangeRemoved(args.index, args.count)
            CollectionChangedType.Update -> notifyItemRangeChanged(args.index, args.count)
            CollectionChangedType.Reset -> notifyDataSetChanged()
        }
    }

}
