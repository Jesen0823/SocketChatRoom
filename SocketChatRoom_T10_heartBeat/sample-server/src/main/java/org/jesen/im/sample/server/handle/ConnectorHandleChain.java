package org.jesen.im.sample.server.handle;

public abstract class ConnectorHandleChain<M> {
    private volatile ConnectorHandleChain<M> next;

    public ConnectorHandleChain<M> appendLast(ConnectorHandleChain<M> newNode) {
        if (newNode == this || this.getClass().equals(newNode.getClass())) {
            return this;
        }
        synchronized (this) {
            if (next == null) {
                next = newNode;
                return newNode;
            }
            return next.appendLast(newNode);
        }
    }

    /**
     * 移除节点
     * @param clc 待移除节点的Class信息
     */
    public synchronized boolean remove(Class<? extends ConnectorHandleChain<M>> clc) {
        if (this.getClass().equals(clc)) {
            return false;
        }
        synchronized (this) {
            if (next == null) {
                return false;
            } else if (next.getClass().equals(clc)) {
                next = next.next;
                return true;
            } else {
                return next.remove(clc);
            }
        }
    }

    public synchronized boolean handle(ClientHandler handler, M model) {
        ConnectorHandleChain<M> next = this.next;
        // 当前节点消费
        if (consume(handler, model)) {
            return true;
        }
        // 是否被下一节点消费
        boolean consumed = next != null && next.handle(handler, model);
        if (consumed) {
            return true;
        }
        // 再次消费
        return consumeAgain(handler, model);
    }

    protected abstract boolean consume(ClientHandler handler, M model);

    protected boolean consumeAgain(ClientHandler handler, M model) {
        return false;
    }
}
