package org.dev.jesen.audiochatroomas.audio

import android.media.AudioFormat
import net.qiujuer.opus.OpusConstant


/**
 * 基础参数定义
 */
interface AudioContract {
    companion object {
        // 采样率
        const val SAMPLE_RATE: Int = 8000

        // 每次用以压缩的样本数量
        const val FRAME_SIZE: Int = 480

        // 通道 1 or 2
        const val NUM_CHANNELS: Int = 1

        // OPUS 压缩PCM数据结构对应Byte比例
        val OPUS_PCM_STRUCT_SIZE: Int = OpusConstant.OPUS_PCM_STRUCT_SIZE_OF_BYTE

        // 音频输入通道
        val AUDIO_CHANNEL_IN: Int =
            if (NUM_CHANNELS == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO

        // 音频输出通道
        val AUDIO_CHANNEL_OUT: Int =
            if (NUM_CHANNELS == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
    }
}