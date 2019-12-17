package moe.tlaster.weipo.common.collection

import kotlinx.coroutines.CoroutineScope

interface ISupportIncrementalLoading {
    suspend fun loadMoreItemsAsync()
    val hasMoreItems: Boolean
    val scope: CoroutineScope
}

interface ISupportCacheLoading {
    suspend fun loadCachedAsync()
    val scope: CoroutineScope
}

