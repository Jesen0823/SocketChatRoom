package org.jesen.im.sample.server.handle.chain;

import org.jesen.im.sample.server.handle.ClientHandler;
import org.jesen.library.clink.core.Connector;

/**
 * 关闭链接时打印信息
 */
public class DefaultPrintConnectorCloseChain extends ConnectorCloseChain{
    @Override
    protected boolean consume(ClientHandler handler, Connector model) {
        System.out.println(handler.getClientInfo()+" exit! key: "+handler.getKey().toString());
        return false;
    }
}
