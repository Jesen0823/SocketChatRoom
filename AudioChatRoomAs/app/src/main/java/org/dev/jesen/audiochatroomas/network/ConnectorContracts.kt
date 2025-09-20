package org.dev.jesen.audiochatroomas.network

/**
 * 链接基础口令
 */
interface ConnectorContracts {
    companion object {
        // 绑定Stream到一个命令链接
        const val COMMAND_CONNECTOR_BIND: String = "--m c bind "

        // 创建对话房间
        const val COMMAND_AUDIO_CREATE_ROOM: String = "--m a create"

        // 加入对话房间(携带参数)
        const val COMMAND_AUDIO_JOIN_ROOM: String = "--m a join "

        // 主动离开对话房间
        const val COMMAND_AUDIO_LEAVE_ROOM: String = "--m a leave"

        // 回送服务器上的唯一标志
        const val COMMAND_INFO_NAME: String = "--i server "

        // AudioInfo信息前缀
        const val COMMAND_INFO_AUDIO_PREFIX: String = "--i a "

        // 回送语音群名
        const val KEY_COMMAND_INFO_AUDIO_ROOM: String = "room"

        // 回送语音开始
        const val KEY_COMMAND_INFO_AUDIO_START: String = "start"

        // 回送语音结束
        const val KEY_COMMAND_INFO_AUDIO_STOP: String = "stop"

        // 回送语音操作错误
        const val KEY_COMMAND_INFO_AUDIO_ERROR: String = "error"
    }
}