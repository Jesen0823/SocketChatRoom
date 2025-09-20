package org.dev.jesen.audiochatroomas.network

import net.qiujuer.library.clink.box.StringReceivePacket
import net.qiujuer.library.clink.core.Connector
import net.qiujuer.library.clink.core.Packet
import net.qiujuer.library.clink.core.ReceivePacket
import java.io.File
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import kotlin.concurrent.Volatile

/**
 * 具有服务器唯一标示的链接
 */
open class ServerNamedConnector(address: String?, port: Int) : Connector() {
    @Volatile
    private var mServerName: String? = null
    private var mMessageArrivedListener: MessageArrivedListener? = null
    private var mConnectorStatusListener: ConnectorStatusListener? = null

    init {
        val socketChannel = SocketChannel.open()
        val socket = socketChannel.socket()
        // 无延迟发送
        socket.setTcpNoDelay(true)
        // 延迟最重要、带宽其次、之后是链接
        socket.setPerformancePreferences(1, 3, 2)
        // 接收数据缓冲区
        socket.setReceiveBufferSize(1024)
        // 发送数据缓冲区
        socket.setSendBufferSize(256)
        // 连接
        socketChannel.connect(InetSocketAddress(Inet4Address.getByName(address), port))
        // 开启
        setup(socketChannel)
    }

    override fun createNewReceiveFile(l: Long, bytes: ByteArray?): File? {
        return null
    }

    override fun createNewReceiveDirectOutputStream(l: Long, bytes: ByteArray?): OutputStream? {
        return null
    }

    override fun onReceivedPacket(packet: ReceivePacket<*, *>) {
        super.onReceivedPacket(packet)
        if (packet.type() == Packet.TYPE_MEMORY_STRING) {
            val entity = (packet as StringReceivePacket).entity()
            if (entity.startsWith(ConnectorContracts.COMMAND_INFO_NAME)) {
                synchronized(this) {
                    // 接收服务器返回的名称
                    mServerName = entity.substring(ConnectorContracts.COMMAND_INFO_NAME.length)
                    (this as Object).notifyAll()
                }
            } else if (entity.startsWith(ConnectorContracts.COMMAND_INFO_AUDIO_PREFIX)) {
                if (mMessageArrivedListener != null) {
                    val msg = entity.substring(ConnectorContracts.COMMAND_INFO_AUDIO_PREFIX.length)
                    val info = ConnectorInfo(msg)
                    mMessageArrivedListener!!.onNewMessageArrived(info)
                }
            }
        }
    }

    override fun onChannelClosed(channel: SocketChannel?) {
        super.onChannelClosed(channel)
        if (mConnectorStatusListener != null) {
            mConnectorStatusListener!!.onConnectorClosed(this)
        }
    }

    @get:Synchronized
    val serverName: String?
        /**
         * 获取服务器的名称
         *
         * @return 服务器标示
         */
        get() {
            if (mServerName == null) {
                try {
                    (this as Object).wait()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            return mServerName
        }

    /**
     * 设置消息监听
     *
     * @param messageArrivedListener 回调
     */
    fun setMessageArrivedListener(messageArrivedListener: MessageArrivedListener?) {
        mMessageArrivedListener = messageArrivedListener
    }

    /**
     * 设置状态监听
     *
     * @param connectorStatusListener 状态变化监听
     */
    fun setConnectorStatusListener(connectorStatusListener: ConnectorStatusListener?) {
        mConnectorStatusListener = connectorStatusListener
    }

    /**
     * 当消息信息到达时回调
     */
    interface MessageArrivedListener {
        fun onNewMessageArrived(info: ConnectorInfo?)
    }


    /**
     * 链接状态监听
     */
    interface ConnectorStatusListener {
        fun onConnectorClosed(connector: Connector?)
    }
}
