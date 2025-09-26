package org.jesen.im.sample.server;

import org.jesen.im.sample.foo.handle.ConnectorHandler;
import org.jesen.im.sample.foo.handle.chain.ConnectorStringPacketChain;
import org.jesen.library.clink.box.StringReceivePacket;

/**
 * 统计数据
 */
public class ServerStatistics {
    long receiveSize;
    long sendSize;

    ConnectorStringPacketChain statisticsChain() {
        return new StatisticsConnectorStringPacketChain();
    }

    /**
     * 接收数据的责任链节点，添加到首节点之后，则可以在每次收到消息时得到回调
     * 然后可以进行接收消息的统计
     */
    class StatisticsConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            // 接收数据量自增
            receiveSize++;
            return false;
        }
    }
}
