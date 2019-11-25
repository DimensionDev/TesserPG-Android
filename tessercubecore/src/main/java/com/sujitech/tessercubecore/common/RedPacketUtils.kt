package com.sujitech.tessercubecore.common

import java.util.*

private val RED_PACKET_BEGIN = "-----BEGIN RED PACKET-----"
private val RED_PACKET_END = "-----END RED PACKET-----"

object RedPacketUtils {

    fun check(value: String): Boolean {
        return value.startsWith(RED_PACKET_BEGIN + System.lineSeparator()) && value.endsWith(RED_PACKET_END)
    }

    fun parse(value: String): RedPacketInfo {
        if (!check(value)) {
            throw IllegalArgumentException("value must start with $RED_PACKET_BEGIN and end with $RED_PACKET_END")
        }
        val results = value.split(System.lineSeparator()).filter {
            it != RED_PACKET_BEGIN && it != RED_PACKET_END
        }
        results.drop(2).forEach {
            kotlin.runCatching {
                UUID.fromString(it)
            }.onFailure {
                throw kotlin.IllegalArgumentException("UUID format error")
            }
        }
        return RedPacketInfo(
                senderFingerprint = results[0].split(':')[0],
                senderId = results[0].split(':')[1],
                contractAddress = results[1],
                uuids = results.drop(2)
        )

    }
}

data class RedPacketInfo(
        val senderFingerprint: String,
        val senderId: String,
        val contractAddress: String,
        val uuids: List<String>
) {
    override fun toString(): String {
        return RED_PACKET_BEGIN +
                System.lineSeparator() +
                senderFingerprint.toUpperCase(Locale.US) + ":" + senderId +
                System.lineSeparator() +
                contractAddress +
                System.lineSeparator() +
                uuids.joinToString(System.lineSeparator()) +
                System.lineSeparator() +
                RED_PACKET_END
    }
}