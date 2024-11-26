package net.qiujuer.lesson.sample.foo.handle;

import net.qiujuer.library.clink.core.Connector;

/**
 * 关闭链接链式结构时打印信息
 */
class DefaultPrintConnectorCloseChain extends ConnectorCloseChain {
    @Override
    protected boolean consume(ConnectorHandler handler, Connector model) {
        System.out.println(handler.getClientInfo()+": Exit, key: "+handler.getKey().toString());
        return false;
    }
}
