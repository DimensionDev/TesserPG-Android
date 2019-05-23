package com.sujitech.tessercubecore.common

class Event<T> {
    private val listeners = ArrayList<(sender: Any, arg: T) -> Unit>()
    fun invoke(sender: Any, arg: T) {
        listeners.forEach { it.invoke(sender, arg) }
    }

    operator fun plusAssign(propertyChanged: (Any, T) -> Unit) {
        listeners.add(propertyChanged)
    }

    operator fun minusAssign(propertyChanged: (Any, T) -> Unit) {
        listeners.remove(propertyChanged)
    }

    fun clear() {
        listeners.clear()
    }
    fun any(): Boolean {
        return listeners.any()
    }
}