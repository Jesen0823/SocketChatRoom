package net.qiujuer.lesson.sample.server.handle;

import net.qiujuer.library.clink.box.StringReceivePacket;

/**
 * 默认String接收节点，不做任何事情
 */
public class DefaultNonConnectorStringChain extends ConnectorStringPacketChain{
    @Override
    protected boolean consume(ClientHandler handler, StringReceivePacket model) {
        return false;
    }
}
