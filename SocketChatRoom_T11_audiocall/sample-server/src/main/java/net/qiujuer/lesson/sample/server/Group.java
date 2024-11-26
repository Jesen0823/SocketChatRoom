package net.qiujuer.lesson.sample.server;

import net.qiujuer.lesson.sample.foo.handle.ConnectorHandler;
import net.qiujuer.lesson.sample.foo.handle.ConnectorStringPacketChain;
import net.qiujuer.library.clink.box.StringReceivePacket;

import java.util.ArrayList;
import java.util.List;

class Group {
    private final String name;
    private final List<ConnectorHandler> memberList = new ArrayList<>();
    private final GroupMessageAdapter adapter;

    public Group(String name, GroupMessageAdapter adapter) {
        this.name = name;
        this.adapter = adapter;
    }

    public String getName() {
        return name;
    }

    boolean addMember(ConnectorHandler connectorHandler) {
        synchronized (memberList) {
            if (!memberList.contains(connectorHandler)) {
                memberList.add(connectorHandler);
                connectorHandler.getStringPacketChain().appendLast(new ForwardConnectorStringPacketChain());

                System.out.println("Group[" + name + "] add new member: " + connectorHandler.getClientInfo());
                return true;
            }
        }
        return false;
    }

    boolean removeMember(ConnectorHandler connectorHandler) {
        synchronized (memberList) {
            if (memberList.remove(connectorHandler)) {
                connectorHandler.getStringPacketChain().remove(ForwardConnectorStringPacketChain.class);
                System.out.println("Group[" + name + "] leave a member: " + connectorHandler.getClientInfo());
                return true;
            }
        }
        return false;
    }

    /**
     * 消息转发链节点
     */
    private class ForwardConnectorStringPacketChain extends ConnectorStringPacketChain {

        /**
         * 消息消费
         *
         * @return true 表示消息被消费
         */
        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            synchronized (memberList) {
                for (ConnectorHandler member : memberList) {
                    if (member == handler) {
                        continue;
                    }
                    adapter.sendMessageToClient(member, stringReceivePacket.entity());
                }
                return true;
            }
        }
    }

    interface GroupMessageAdapter {
        void sendMessageToClient(ConnectorHandler handler, String msg);
    }
}
