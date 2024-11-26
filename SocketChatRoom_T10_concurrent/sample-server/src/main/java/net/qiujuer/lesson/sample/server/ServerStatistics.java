package net.qiujuer.lesson.sample.server;

import net.qiujuer.lesson.sample.foo.handle.ConnectorHandler;
import net.qiujuer.lesson.sample.foo.handle.ConnectorStringPacketChain;
import net.qiujuer.library.clink.box.StringReceivePacket;

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
