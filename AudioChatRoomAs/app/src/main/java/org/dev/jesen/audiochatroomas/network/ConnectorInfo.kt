package org.dev.jesen.audiochatroomas.network

/**
 * 基础返回信息Model
 */
class ConnectorInfo internal constructor(msg: String) {
    val key: String?
    val value: String?

    init {
        val strings: Array<String?> =
            msg.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        key = strings[0]
        if (strings.size == 2) {
            value = strings[1]
        } else {
            value = null
        }
    }
}
