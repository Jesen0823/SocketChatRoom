package org.dev.jesen.audiochatroomas

import org.dev.jesen.audiochatroomas.network.LocalIpUtils

/**
 * 公共契约
 */
interface AppContract {
    interface View {
        fun showProgressDialog(string: Int)

        fun dismissProgressDialog()

        fun showToast(string: Int)

        /**
         * 显示房间号
         *
         * @param code 房间号
         */
        fun showRoomCode(code: String?)

        /**
         * 在线
         */
        fun onOnline()

        /**
         * 离线
         */
        fun onOffline()
    }

    interface Presenter {
        /**
         * 主动离开房间
         */
        fun leaveRoom()

        /**
         * 加入已有房间
         *
         * @param code 房间号
         */
        fun joinRoom(code: String?)

        /**
         * 创建房间
         */
        fun createRoom()

        /**
         * 退出APP操作
         */
        fun destroy()
    }

    companion object {
        // 服务器地址
        var SERVER_ADDRESS: String = "192.168.1.12"
        init {
            //SERVER_ADDRESS = LocalIpUtils.getLocalIpv4Address()
        }

        // 服务器端口
        const val PORT: Int = 30401
    }
}
