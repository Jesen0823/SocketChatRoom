package org.jesen.im.sample.server;

import org.jesen.im.sample.foo.handle.ConnectorHandler;
import org.jesen.im.sample.foo.handle.ConnectorStringPacketChain;
import org.jesen.library.clink.box.StringReceivePacket;

/**
 * 数据统计类
 */
public class ServerStatistics {
    long receiveSize;
    long sendSize;

    ConnectorStringPacketChain staticsChain(){
        return new StatisticsConnectorStringPacketChain();
    }

    class StatisticsConnectorStringPacketChain extends ConnectorStringPacketChain{

        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket model) {
            // 接收数据
            receiveSize++;
            return false;
        }
    }
}
