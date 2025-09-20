package org.dev.jesen.audiochatroomas.network

import java.net.*
import java.util.Enumeration

object LocalIpUtils {

    /**
     * 获取设备当前的局域网IPv4地址（优先返回Wi-Fi地址）
     * @return 有效的IPv4地址（如192.168.1.100），无有效地址时返回"unknown"
     */
    fun getLocalIpv4Address(): String {
        try {
            // 遍历所有网络接口
            val networkInterfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()

                // 过滤条件：
                // 1. 不是回环接口（lo接口，地址通常为127.0.0.1）
                // 2. 接口必须处于启用状态
                if (networkInterface.isLoopback || !networkInterface.isUp) {
                    continue
                }

                // 遍历接口下的所有IP地址
                val inetAddresses: Enumeration<InetAddress> = networkInterface.inetAddresses
                while (inetAddresses.hasMoreElements()) {
                    val inetAddress = inetAddresses.nextElement()

                    // 只保留IPv4地址，且不是回环地址
                    if (inetAddress is Inet4Address && !inetAddress.isLoopbackAddress) {
                        // 优先返回Wi-Fi接口的地址（接口名称通常包含"wlan"）
                        if (networkInterface.displayName.contains("wlan", ignoreCase = true)) {
                            return inetAddress.hostAddress ?: "unknown"
                        }
                        // 其他接口的IPv4地址（如以太网）作为备选
                        return inetAddress.hostAddress ?: "unknown"
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        return "unknown"
    }
}
