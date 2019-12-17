package moe.tlaster.weipo.common.collection

class CollectionChangedEventArg(
    val type: CollectionChangedType,
    val index: Int = -1,
    val count: Int = 1
)