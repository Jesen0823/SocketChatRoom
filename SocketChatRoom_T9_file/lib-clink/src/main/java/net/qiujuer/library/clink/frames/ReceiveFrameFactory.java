package net.qiujuer.library.clink.frames;

import net.qiujuer.library.clink.core.Frame;
import net.qiujuer.library.clink.core.IoArgs;

/**
 * 接收帧构建工厂
 */
public class ReceiveFrameFactory {
    /**
     * 使用传入的帧头数据构建接收帧
     *
     * @param args IoArgs至少需要有6字节数据可读
     * @return 构建的帧头数据
     */
    public static AbsReceiveFrame createInstance(IoArgs args) {
        byte[] buffer = new byte[Frame.FRAME_HEADER_LENGTH];
        args.writeTo(buffer, 0);
        byte type = buffer[2];
        switch (type) {
            case Frame.TYPE_PACKET_HEADER:
                return new ReceiveHeaderFrame(buffer);
            case Frame.TYPE_PACKET_ENTITY:
                return new ReceiveEntityFrame(buffer);
            case Frame.TYPE_COMMAND_SEND_CANCEL:
                return new CancelReceiveFrame(buffer);
            default:
                throw new UnsupportedOperationException("Unsupported frame type:" + type);
        }
    }

}

