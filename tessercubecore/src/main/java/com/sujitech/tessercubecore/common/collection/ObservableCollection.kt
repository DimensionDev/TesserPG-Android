package com.sujitech.tessercubecore.common.collection

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.function.Predicate

interface INotifyCollectionChanged {
    var collectionChanged: LiveData<CollectionChangedEventArg>
}

open class ObservableCollection<T> : ArrayList<T>(), INotifyCollectionChanged {

    private val _collectionChanged = MutableLiveData<CollectionChangedEventArg>()
    override var collectionChanged: LiveData<CollectionChangedEventArg> = _collectionChanged

    override fun add(element: T): Boolean {
        val result = super.add(element)
        if (result) {
            _collectionChanged.value = CollectionChangedEventArg(CollectionChangedType.Add, this.count() - 1, 1)
        }
        return result
    }

    override fun add(index: Int, element: T) {
        super.add(index, element)
        _collectionChanged.value = CollectionChangedEventArg(CollectionChangedType.Add, index, 1)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val result = super.addAll(elements)
        if (result) {
            _collectionChanged.value = CollectionChangedEventArg(CollectionChangedType.Add, this.count() - elements.count(), elements.count())
        }
        return result
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val result = super.addAll(index, elements)
        if (result) {
            _collectionChanged.value = CollectionChangedEventArg(CollectionChangedType.Add, index, elements.count())
        }
        return result
    }

    override fun clear() {
        val size = this.size
        super.clear()
        _collectionChanged.value = CollectionChangedEventArg(CollectionChangedType.Reset, 0, size)
    }

    override fun remove(element: T): Boolean {
        val index = indexOf(element)
        val result = super.remove(element)
        if (result) {
            _collectionChanged.value = CollectionChangedEventArg(CollectionChangedType.Remove, index, 1)
        }
        return result
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        val result = super.removeAll(elements)
        if (result) {
            _collectionChanged.value = CollectionChangedEventArg(CollectionChangedType.Remove)
        }
        return result
    }

    override fun removeAt(index: Int): T {
        val result = super.removeAt(index)
        if (result != null) {
            _collectionChanged.value = CollectionChangedEventArg(CollectionChangedType.Remove, index, 1)
        }
        return result
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun removeIf(filter: Predicate<in T>): Boolean {
        val index = indexOfFirst { filter.test(it) }
        val result = super.removeIf(filter)
        if (result) {
            _collectionChanged.value = CollectionChangedEventArg(CollectionChangedType.Remove, index, 1)
        }
        return result
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        super.removeRange(fromIndex, toIndex)
        _collectionChanged.value = CollectionChangedEventArg(CollectionChangedType.Remove, fromIndex, toIndex - fromIndex)
    }
}