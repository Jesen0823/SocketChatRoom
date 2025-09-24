package org.jesen.im.sample.foo.handle.chain;

import org.jesen.im.sample.foo.handle.ConnectorHandler;
import org.jesen.library.clink.box.StringReceivePacket;

/**
 * 默认String接收节点，不做任何事
 */
public class DefaultNonConnectorStringPacketChain extends ConnectorStringPacketChain {
    @Override
    protected boolean consume(ConnectorHandler handler, StringReceivePacket model) {
        return false;
    }
}
