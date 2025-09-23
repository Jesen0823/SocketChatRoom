package org.jesen.im.sample.server.handle.chain;

import org.jesen.im.sample.server.handle.ClientHandler;
import org.jesen.library.clink.box.StringReceivePacket;

/**
 * 默认String接收节点，不做任何事
 */
public class DefaultNonConnectorStringPacketChain extends ConnectorStringPacketChain {
    @Override
    protected boolean consume(ClientHandler handler, StringReceivePacket model) {
        return false;
    }
}
