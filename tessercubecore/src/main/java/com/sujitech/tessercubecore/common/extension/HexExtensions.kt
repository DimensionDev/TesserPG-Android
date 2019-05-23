package com.sujitech.tessercubecore.common.extension

import kotlin.math.absoluteValue

fun String.toFormattedHexText(): String {
    return toUpperCase().chunked(4).joinToString(" ")
}

fun String.splitTo(numberEachLine: Int): String {
    return split(" ").chunked(numberEachLine).map { it.joinToString(" ") }.joinToString(System.lineSeparator())
}

fun Long.toFormattedHexText(): String {
    return absoluteValue.toString(16).toFormattedHexText()
}