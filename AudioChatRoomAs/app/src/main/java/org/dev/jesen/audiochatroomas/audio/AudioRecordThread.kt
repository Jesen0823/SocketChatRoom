package org.dev.jesen.audiochatroomas.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import net.qiujuer.opus.OpusEncoder
import org.dev.jesen.audiochatroomas.MyApplication
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
/**
 * 声音收集线程
 */

class AudioRecordThread(private val mOutputStream: OutputStream) : Thread() {
    val audioRecord: AudioRecord

    init {
        // 麦克风内部缓冲区大小
        val minBufferSize = AudioRecord.getMinBufferSize(
            AudioContract.SAMPLE_RATE,
            AudioContract.AUDIO_CHANNEL_IN,
            AudioFormat.ENCODING_PCM_16BIT
        )

        // 初始化录音器
        if (ActivityCompat.checkSelfPermission(
                MyApplication.instance.appContext,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        this.audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, AudioContract.SAMPLE_RATE,
            AudioContract.AUDIO_CHANNEL_IN, AudioFormat.ENCODING_PCM_16BIT, minBufferSize
        )
    }

    @SuppressLint("WrongConstant")
    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE)

        val audioRecord = this.audioRecord

        // 原始PCM数据
        val pcmBuffer =
            ByteArray(AudioContract.FRAME_SIZE * AudioContract.NUM_CHANNELS * AudioContract.OPUS_PCM_STRUCT_SIZE)
        // 压缩后的数据， 前2字节用以存储压缩长度，从第3字节开始存储压缩数据
        val encodeBuffer = ByteArray(256)
        // 用以快速设置长度信息的包裹用法
        val encodeSizeBuffer = ByteBuffer.wrap(encodeBuffer, 0, 2)

        // 压缩器
        val encoder = OpusEncoder(
            AudioContract.SAMPLE_RATE,
            AudioContract.NUM_CHANNELS,
            OpusEncoder.OPUS_APPLICATION_VOIP
        )
        encoder.setComplexity(4)

        // 开始
        audioRecord.startRecording()

        try {
            while (!interrupted()) {
                // 本次需要读取的数据总大小，填满缓冲区
                var canReadSize = pcmBuffer.size
                // 已读大小
                var readSize = 0
                while (canReadSize > 0) {
                    // 每次读取大小
                    val onceReadSize = audioRecord.read(pcmBuffer, readSize, canReadSize)
                    if (onceReadSize < 0) {
                        throw RuntimeException("recorder.read() returned error:" + onceReadSize)
                    }
                    canReadSize -= onceReadSize
                    readSize += onceReadSize
                }

                // 压缩数据，存储区间从位移2个字节后开始
                val encodeSize =
                    encoder.encode(pcmBuffer, 0, encodeBuffer, 2, AudioContract.FRAME_SIZE)

                // 存储大小信息
                encodeSizeBuffer.clear()
                encodeSizeBuffer.putShort(encodeSize.toShort())

                // 发送数据
                mOutputStream.write(encodeBuffer, 0, encodeSize + 2)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            audioRecord.stop()
            audioRecord.release()
            encoder.release()
        }
    }
}
