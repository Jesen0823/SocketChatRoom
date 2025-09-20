package org.jesen.library.clink.core.ds;

/**
 * 带优先级的节点，可用于构成链表
 */
public class BytePriorityNode<Item> {
    public byte priority;
    public Item item; // 节点要存储的数据
    public BytePriorityNode<Item> next;

    public BytePriorityNode(Item item) {
        this.item = item;
    }

    public void appendWithPriority(BytePriorityNode<Item> node) {
        if (next == null) {
            next = node;
        } else {
            BytePriorityNode<Item> after = this.next;
            if (after.priority < node.priority) {
                // 中间位置插入
                this.next = node;
                node.next = after;
            } else {
                after.appendWithPriority(node);
            }
        }
    }
}
