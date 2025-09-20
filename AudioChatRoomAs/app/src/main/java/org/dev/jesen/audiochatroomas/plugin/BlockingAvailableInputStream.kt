package org.dev.jesen.audiochatroomas.plugin

import java.io.IOException
import java.io.InputStream

/**
 * 阻塞可读性InputStream，当不可读时将阻塞到有数据可读
 */
class BlockingAvailableInputStream(private val inputStream: InputStream) : InputStream() {
    @Throws(IOException::class)
    override fun read(b: ByteArray?): Int {
        return inputStream.read(b)
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        return inputStream.read(b, off, len)
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        return inputStream.skip(n)
    }

    @Throws(IOException::class)
    override fun close() {
        inputStream.close()
    }

    @Synchronized
    override fun mark(readlimit: Int) {
        inputStream.mark(readlimit)
    }

    @Synchronized
    @Throws(IOException::class)
    override fun reset() {
        inputStream.reset()
    }

    override fun markSupported(): Boolean {
        return inputStream.markSupported()
    }

    @Throws(IOException::class)
    override fun read(): Int {
        return inputStream.read()
    }

    @Throws(IOException::class)
    override fun available(): Int {
        do {
            val available = inputStream.available()
            if (available == 0) {
                // 可读数据为0时, 放弃当前的循环机会，等待下一次CPU调度
                Thread.yield()
            } else {
                // 返回有数据可读，或-1停止
                return available
            }
        } while (true)
    }
}