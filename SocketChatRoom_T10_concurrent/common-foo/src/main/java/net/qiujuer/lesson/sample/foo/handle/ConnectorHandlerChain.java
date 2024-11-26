package net.qiujuer.lesson.sample.foo.handle;

/**
 * 责任链默认结构
 */
public abstract class ConnectorHandlerChain<M> {
    // 当前节点的下一个节点
    private volatile ConnectorHandlerChain<M> next;

    /**
     * 添加一个新节点到链的末端
     *
     * @param newChain 新的节点
     * @return 返回新节点
     */
    public ConnectorHandlerChain<M> appendLast(ConnectorHandlerChain<M> newChain) {
        // 如果当前节点和新增节点相同，不允许添加，链上只能存在某一节点的一个实例
        if (newChain == this || this.getClass().equals(newChain.getClass())) {
            return this;
        }
        synchronized (this) {
            if (next == null) {
                next = newChain;
                return newChain;
            }
            return next.appendLast(newChain);
        }
    }

    /**
     * 移除某一节点及其之后的节点
     * 另外，移除某节点时，如果它有后续节点，则将后续节点接到当前节点上，也就是移除中间那个节点
     *
     * @param clx 待移除节点的Class
     * @return 是否移除成功
     */
    public synchronized boolean remove(Class<? extends ConnectorHandlerChain<M>> clx) {
        // 自己不能移除自己
        if (this.getClass().equals(clx)) {
            return false;
        }
        synchronized (this) {
            if (next == null) {
                return false;
            } else if (next.getClass().equals(clx)) {
                // 移除next节点
                next = next.next;
                return true;
            } else {
                // 交给next进行移除操作
                return next.remove(clx);
            }
        }
    }

    synchronized boolean handle(ClientHandler handler, M model) {
        ConnectorHandlerChain<M> next = this.next;
        if (consume(handler, model)) {
            return true;
        }
        boolean consumed = next != null && next.handle(handler, model);
        if (consumed) {
            return true;
        }
        return consumeAgain(handler, model);
    }

    protected abstract boolean consume(ClientHandler handler, M model);

    protected boolean consumeAgain(ClientHandler handler, M model) {
        return false;
    }
}
