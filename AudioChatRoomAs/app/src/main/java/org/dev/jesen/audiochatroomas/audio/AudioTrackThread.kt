package org.dev.jesen.audiochatroomas.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.audiofx.AcousticEchoCanceler
import android.os.Process
import net.qiujuer.opus.OpusDecoder
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer


/**
 * 声音收集线程
 */
class AudioTrackThread(private val mInputStream: InputStream, audioSessionId: Int) : Thread() {
    private val mAudioTrack: AudioTrack
    private var mAcousticEchoCanceler: AcousticEchoCanceler? = null

    init {
        // 播放器内部缓冲区大小
        val minBufferSize = AudioTrack.getMinBufferSize(
            AudioContract.SAMPLE_RATE,
            AudioContract.AUDIO_CHANNEL_OUT,
            AudioFormat.ENCODING_PCM_16BIT
        )

        // 初始化播放器
        mAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            AudioContract.SAMPLE_RATE,
            AudioContract.AUDIO_CHANNEL_OUT,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize,
            AudioTrack.MODE_STREAM,
            audioSessionId
        )

        // 初始化回音消除
        try {
            val acousticEchoCanceler = AcousticEchoCanceler.create(audioSessionId)
            acousticEchoCanceler.enabled = true
            mAcousticEchoCanceler = acousticEchoCanceler
        } catch (e: Exception) {
            mAcousticEchoCanceler = null
        }
    }

    @SuppressLint("WrongConstant")
    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE)

        val audioTrack = mAudioTrack

        // 原始PCM数据
        val pcmBuffer =
            ByteArray(AudioContract.FRAME_SIZE * AudioContract.NUM_CHANNELS * AudioContract.OPUS_PCM_STRUCT_SIZE)
        // 压缩后的数据
        val encodeBuffer = ByteArray(1024)
        // 分配2字节用以读取每次压缩的数据体大小
        val encodeSizeBuffer = ByteBuffer.allocate(2)

        // 解压
        val decoder = OpusDecoder(AudioContract.SAMPLE_RATE, AudioContract.NUM_CHANNELS)

        // 开始
        audioTrack.play()

        try {
            while (!interrupted()) {
                // 获取前2字节，用以计算长度
                encodeSizeBuffer.clear()
                fullData(mInputStream, encodeSizeBuffer.array(), 2)
                encodeSizeBuffer.position(2)
                encodeSizeBuffer.flip()
                val encodeSize = encodeSizeBuffer.getShort().toInt()

                // 填充压缩数据体
                if (!fullData(mInputStream, encodeBuffer, encodeSize)) {
                    continue
                }

                // 解压数据
                val pcmSize =
                    decoder.decode(encodeBuffer, encodeSize, pcmBuffer, AudioContract.FRAME_SIZE)

                // 播放
                audioTrack.write(pcmBuffer, 0, pcmSize)
                audioTrack.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            audioTrack.stop()
            audioTrack.release()
            decoder.release()
            if (mAcousticEchoCanceler != null) {
                mAcousticEchoCanceler!!.release()
            }
        }
    }


    @Throws(IOException::class)
    private fun fullData(inputStream: InputStream, bytes: ByteArray?, size: Int): Boolean {
        var readSize = 0
        do {
            val read = inputStream.read(bytes, readSize, size - readSize)
            if (read == -1) {
                return false
            }
            readSize += read
        } while (readSize < size)
        return true
    }
}
