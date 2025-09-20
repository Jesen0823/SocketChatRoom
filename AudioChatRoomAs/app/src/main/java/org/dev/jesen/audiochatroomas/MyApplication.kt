package org.dev.jesen.audiochatroomas

import android.app.Application
import org.dev.jesen.audiochatroomas.network.LocalIpUtils

class MyApplication : Application() {

    // 伴生对象保存全局实例
    companion object {
        // 用lateinit延迟初始化，避免空安全检查
        lateinit var instance: MyApplication
            private set // 私有setter，防止外部修改
    }

    override fun onCreate() {
        super.onCreate()
        // 初始化实例为当前Application对象
        instance = this
        //AppContract.SERVER_ADDRESS = LocalIpUtils.getLocalIpv4Address()
    }

    // 提供获取全局ApplicationContext的方法（可选，简化调用）
    val appContext: android.content.Context
        get() = applicationContext // Application的applicationContext就是自身
}
