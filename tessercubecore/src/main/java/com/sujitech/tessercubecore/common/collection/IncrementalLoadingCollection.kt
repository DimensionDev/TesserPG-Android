package moe.tlaster.weipo.common.collection

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class IncrementalLoadingCollection<TSource: IIncrementalSource<T>, T>(
    private val source: TSource,
    private val itemsPerPage: Int = 20,
    override val scope: CoroutineScope = GlobalScope
): ObservableCollection<T>(), ISupportIncrementalLoading, ISupportCacheLoading {

    val onError = MutableLiveData<Throwable>()

    enum class CollectionState {
        Loading,
        Completed
    }

    protected var currentPageIndex = 0
    val stateChanged = MutableLiveData<CollectionState>()
    var isLoading = false

    fun refresh() {
        scope.launch {
            refreshAsync()
        }
    }

    suspend fun refreshAsync() {
        currentPageIndex = 0
        hasMoreItems = true
        loadMoreItemsAsync()
    }

    override suspend fun loadMoreItemsAsync() {
        if (isLoading) {
            return
        }
        isLoading = true
        stateChanged.value = CollectionState.Loading
        var result: List<T>? = null
        val lazyClear = currentPageIndex == 0
        try {
            result = source.getPagedItemAsync(currentPageIndex++, itemsPerPage)
        } catch (e: Throwable) {
            onError. value = e
            e.printStackTrace()
        }
        if (lazyClear) {
            clear()
        }
        if (result != null && result.any()) {
            addAll(result)
        } else {
            hasMoreItems = false
        }
        stateChanged.value = CollectionState.Completed
        isLoading = false
    }

    override suspend fun loadCachedAsync() {
        if (source !is ICachedIncrementalSource<*>) {
            return
        }
        if (isLoading) {
            return
        }
        isLoading = true
        stateChanged.value = CollectionState.Loading
        kotlin.runCatching {
            source.getCachedItemsAsync()
        }.onFailure {
            onError.value = it
            it.printStackTrace()
        }.onSuccess {
            addAll(it.map { it as T })
        }
        stateChanged.value = CollectionState.Completed
        isLoading = false
    }

    override var hasMoreItems: Boolean = true
}
