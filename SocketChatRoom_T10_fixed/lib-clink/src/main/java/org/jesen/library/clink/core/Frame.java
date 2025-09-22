package org.jesen.library.clink.core;

import java.io.IOException;

public abstract class Frame {
    public static final int FRAME_HEADER_LENGTH = 6;
    protected final byte[] header = new byte[FRAME_HEADER_LENGTH];
    public static final int MAX_CAPACITY = 64 * 1024 - 1;

    public static final byte TYPE_PACKET_HEADER = 11;
    public static final byte TYPE_PACKET_ENTITY = 12;

    public static final byte TYPE_COMMAND_SEND_CANCEL = 41;
    public static final byte TYPE_COMMAND_RECEIVE_REJECT = 42;

    public static final byte FLAG_NONE = 0;

    public Frame(int length, byte type, byte flag, short identifier) {
        if (length < 0 || length > MAX_CAPACITY) {
            throw new RuntimeException("The Body length of a single frame should be between 0 and 65535");
        }
        if (identifier < 1 || identifier > 255) {
            throw new RuntimeException("The Body identifier of a single frame should be between 1 and 255");
        }
        // 长度，高位在前
        /*
         * 分析：
         * 假设 length = 65535; 32位二进制 00000000 00000000 11111111 11111111
         * 假设 byte type = 12; 8位二进制 00001100
         * 假设 byte flag = 0;  8位二进制 00000000
         * 假设 short identifier = 18; 16位二进制 00000000 00010010
         * byte[] header = [0,0,0,0,0,0] 长度6
         * 经过运算 header = [-1,-1,12,0,18,0] 十进制
         * [11111111,11111111,00001100,00000000,00010010,00000000]
         */
        header[0] = (byte) (length >> 8);
        /* length >> 8：将length（32 位）右移 8 位，保留高 16 位中的高 8 位（即原 16 位长度的高 8 位）
         * 00000000 00000000 11111111 （右移8位） 11111111
         * 00000000 00000000 00000000 11111111
         * (byte)强转：取低 8 位作为 byte（8 位），结果为11111111
         * 注意：byte 是有符号类型，11111111表示十进制-1（补码规则）
         * 结果：header[0] = (byte) 0xFF = -1（二进制11111111）
         */

        header[1] = (byte) (length);
        /*
         * (byte) length：直接取length的低 8 位作为 byte（高 24 位被截断）
         * 结果：header[1] = (byte) 0xFF = -1（二进制11111111）
         */

        header[2] = type;
        /*
         * 直接赋值，type的二进制为00001100，故header[2] = 12（二进制00001100）
         */

        header[3] = flag;
        /*
         * 直接赋值，flag的二进制为00000000，故header[3] = 0（二进制00000000）
         */

        // 唯一标识，取short的后8个位
        header[4] = (byte) identifier;
        /*
         * identifier是 16 位 short，二进制为00000000 00010010，强转 byte 时取低 8 位：00010010
         * 结果：header[4] = 18（二进制00010010）
         */

        // 预留空间，以0填充
        header[5] = 0;
        /*
         * 直接赋值 0，二进制00000000
         */
    }

    public Frame(byte[] header) {
        System.arraycopy(header, 0, this.header, 0, FRAME_HEADER_LENGTH);
    }

    /*
     * 自定义协议中的 “长度字段” 解析，接收方可以还原数据长度
     * & 0xFF解决了byte（8 位有符号）与int（32 位有符号）转换时的 “符号扩展” 问题
     * */
    public int getBodyLength() {
        // 拼接后转为一个int，高位会自动补成1，所以先做&操作，将高位变回0，再拼接，转为int返回
        return ((((int) header[0]) & 0xFF) << 8) | (((int) header[1]) & 0xFF);
        /*
         * header[0]:
         * header[0]是 byte 类型的-1（二进制11111111）
         * 转为 int 时，Java 会进行 “符号扩展”（高位补符号位 1），
         * 结果为 32 位 int：11111111 11111111 11111111 11111111（十进制-1）
         * (((int) header[0]) & 0xFF):
         * 0xFF的 32 位二进制是00000000 00000000 00000000 11111111
         * 按位与（&）操作：保留低 8 位，高 24 位清零，结果为：
         *                   00000000 00000000 00000000 11111111（十进制255）
         * ((((int) header[0]) & 0xFF) << 8):
         * 结果左移 8 位，低 8 位补 0，结果为：
         * 00000000 00000000 11111111 00000000（十进制255 × 256 = 65280）
         * |
         * (int) header[1]:
         * 符号扩展后为11111111 11111111 11111111 11111111
         * ((int) header[1]) & 0xFF：
         * 0xFF二进制是00000000 00000000 00000000 11111111
         * 结果为      00000000 00000000 00000000 11111111（十进制255）
         * 最后，|操作（拼接高低 8 位）：
         * 00000000 00000000 11111111 11111111（十进制65535）
         * */
    }

    public byte getBodyType() {
        return header[2];
    }

    public byte getBodyFlag() {
        return header[3];
    }

    // 帧体剩余长度
    public short getBodyIdentifier() {
        return (short) (((short) header[4]) & 0xFF);
    }

    /**
     * 消费数据
     *
     * @return boolean 是否消费完成
     */
    public abstract boolean handle(IoArgs args) throws IOException;

    public abstract Frame nextFrame();

    public abstract int getConsumableLength();
}
