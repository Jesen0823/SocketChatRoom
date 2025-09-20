package org.dev.jesen.audiochatroomas

import android.util.Log
import net.qiujuer.library.clink.box.StreamDirectSendPacket
import net.qiujuer.library.clink.core.Connector
import net.qiujuer.library.clink.core.IoContext
import net.qiujuer.library.clink.impl.IoSelectorProvider
import net.qiujuer.library.clink.impl.SchedulerImpl
import net.qiujuer.library.clink.utils.CloseUtils
import net.qiujuer.library.clink.utils.plugin.CircularByteBuffer
import org.dev.jesen.audiochatroomas.audio.AudioRecordThread
import org.dev.jesen.audiochatroomas.audio.AudioTrackThread
import org.dev.jesen.audiochatroomas.network.ConnectorContracts
import org.dev.jesen.audiochatroomas.network.ConnectorInfo
import org.dev.jesen.audiochatroomas.network.ServerNamedConnector
import org.dev.jesen.audiochatroomas.network.ServerNamedConnector.ConnectorStatusListener
import org.dev.jesen.audiochatroomas.network.ServerNamedConnector.MessageArrivedListener
import org.dev.jesen.audiochatroomas.plugin.BlockingAvailableInputStream
import org.dev.jesen.kit_handler.Run
import org.dev.jesen.kit_handler.runable.Action
import java.io.IOException
import java.io.OutputStream
import kotlin.concurrent.Volatile

/**
 * 业务逻辑处理部分
 */
class Presenter internal constructor(private val mView: AppContract.View) : AppContract.Presenter,
    ConnectorStatusListener {
    // 音频录制缓冲区，录制的声音将缓存到当前缓冲区等待被网络读取发送
    private val mAudioRecordBuffer = CircularByteBuffer(128, true)

    // 音频播放缓冲期，网络接收的语音将存储到当前缓冲区等待被播放器读取播放
    private val mAudioTrackBuffer = CircularByteBuffer(1024, true)

    // 传输命令的链接
    @Volatile
    private var mCmdConnector: ServerNamedConnector? = null

    // 传输具体音频的链接
    @Volatile
    private var mStreamConnector: ServerNamedConnector? = null

    @Volatile
    private var mAudioTrackThread: AudioTrackThread? = null

    @Volatile
    private var mAudioRecordThread: AudioRecordThread? = null

    @Volatile
    private var mAudioRecordStreamDirectSendPacket: StreamDirectSendPacket? = null

    override fun leaveRoom() {
        if (checkConnector()) {
            mView.showProgressDialog(R.string.dialog_loading)
            mCmdConnector!!.send(ConnectorContracts.COMMAND_AUDIO_LEAVE_ROOM)
        }
    }

    override fun joinRoom(code: String?) {
        if (checkConnector()) {
            mView.showProgressDialog(R.string.dialog_loading)
            mCmdConnector!!.send(ConnectorContracts.COMMAND_AUDIO_JOIN_ROOM + code)
        }
    }

    override fun createRoom() {
        if (checkConnector()) {
            mView.showProgressDialog(R.string.dialog_loading)
            mCmdConnector!!.send(ConnectorContracts.COMMAND_AUDIO_CREATE_ROOM)
        }
    }

    override fun destroy() {
        stopAudioThread()
        Run.onBackground(DestroyAction())
    }

    override fun onConnectorClosed(connector: Connector?) {
        destroy()
        dismissDialogAndToast(R.string.toast_connector_closed, false)
    }


    /**
     * 检查当前的连接是否可用
     *
     * @return True 可用
     */
    private fun checkConnector(): Boolean {
        if (mCmdConnector == null || mStreamConnector == null) {
            mView.showToast(R.string.toast_bad_network)
            return false
        }
        return true
    }

    /**
     * 停止Audio
     */
    private fun stopAudioThread() {
        if (mAudioRecordStreamDirectSendPacket != null) {
            // 停止发送包
            CloseUtils.close(mAudioRecordStreamDirectSendPacket!!.open())
            mAudioRecordStreamDirectSendPacket = null
        }

        if (mAudioRecordThread != null) {
            // 停止录制
            mAudioRecordThread!!.interrupt()
            mAudioRecordThread = null
        }

        if (mAudioTrackThread != null) {
            // 停止播放
            mAudioTrackThread!!.interrupt()
            mAudioTrackThread = null
        }

        // 清理缓冲区
        mAudioTrackBuffer.clear()
        mAudioRecordBuffer.clear()
    }

    /**
     * 开始Audio
     */
    private fun startAudioThread() {
        // 发送直流包
        mAudioRecordStreamDirectSendPacket =
            StreamDirectSendPacket(BlockingAvailableInputStream(mAudioRecordBuffer.getInputStream()))
        mStreamConnector!!.send(mAudioRecordStreamDirectSendPacket)

        // 启动音频
        val audioRecordThread = AudioRecordThread(mAudioRecordBuffer.getOutputStream())
        val audioTrackThread = AudioTrackThread(
            BlockingAvailableInputStream(mAudioTrackBuffer.getInputStream()),
            audioRecordThread.audioRecord.getAudioSessionId()
        )

        audioTrackThread.start()
        audioRecordThread.start()

        mAudioTrackThread = audioTrackThread
        mAudioRecordThread = audioRecordThread
    }

    /**
     * 移除弹出框，并且显示Toast
     */
    private fun dismissDialogAndToast(toast: Int, online: Boolean) {
        Run.onUiAsync(object : Action {
            override fun call() {
                mView.dismissProgressDialog()
                mView.showToast(toast)
                if (online) {
                    mView.onOnline()
                } else {
                    mView.onOffline()
                }
            }
        })
    }

    /**
     * 设置群Code到UI层
     *
     * @param code 群标志
     */
    private fun setViewRoomCode(code: String?) {
        Run.onUiAsync(object : Action {
            override fun call() {
                mView.showRoomCode(code)
            }
        })
    }

    private inner class InitAction : Action {
        override fun call() {
            Log.d("XXXX","ip = ${AppContract.SERVER_ADDRESS}")
            try {
                IoContext.setup()
                    .ioProvider(IoSelectorProvider())
                    .scheduler(SchedulerImpl(1))
                    .start()

                mCmdConnector = ServerNamedConnector(AppContract.SERVER_ADDRESS, AppContract.PORT)
                mCmdConnector!!.setMessageArrivedListener(mMessageArrivedListener)
                mCmdConnector!!.setConnectorStatusListener(this@Presenter)

                mStreamConnector =
                    object : ServerNamedConnector(AppContract.SERVER_ADDRESS, AppContract.PORT) {
                        protected override fun createNewReceiveDirectOutputStream(
                            l: Long,
                            bytes: ByteArray?
                        ): OutputStream? {
                            return mAudioTrackBuffer.getOutputStream()
                        }
                    }
                mStreamConnector!!.setConnectorStatusListener(this@Presenter)

                // 发送绑定命令
                mCmdConnector!!.send(ConnectorContracts.COMMAND_CONNECTOR_BIND + mStreamConnector!!.serverName)

                // 成功
                dismissDialogAndToast(R.string.toast_link_succeed, false)
            } catch (e: IOException) {
                // 销毁
                DestroyAction().call()
                // 失败
                dismissDialogAndToast(R.string.toast_link_failed, false)
            }
        }
    }

    private val mMessageArrivedListener: MessageArrivedListener = object : MessageArrivedListener {
        override fun onNewMessageArrived(info: ConnectorInfo?) {
            when (info?.key) {
                ConnectorContracts.KEY_COMMAND_INFO_AUDIO_ROOM -> {
                    setViewRoomCode(info.value)
                    dismissDialogAndToast(R.string.toast_room_name, true)
                }

                ConnectorContracts.KEY_COMMAND_INFO_AUDIO_START -> {
                    startAudioThread()
                    dismissDialogAndToast(R.string.toast_start, true)
                }

                ConnectorContracts.KEY_COMMAND_INFO_AUDIO_STOP -> {
                    stopAudioThread()
                    dismissDialogAndToast(R.string.toast_stop, false)
                }

                ConnectorContracts.KEY_COMMAND_INFO_AUDIO_ERROR -> {
                    stopAudioThread()
                    dismissDialogAndToast(R.string.toast_error, false)
                }
            }
        }
    }

    init {
        mView.showProgressDialog(R.string.dialog_linking)
        Run.onBackground(InitAction())
    }

    private inner class DestroyAction : Action {
        override fun call() {
            CloseUtils.close(mCmdConnector, mStreamConnector)
            mCmdConnector = null
            mStreamConnector = null
            try {
                IoContext.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
