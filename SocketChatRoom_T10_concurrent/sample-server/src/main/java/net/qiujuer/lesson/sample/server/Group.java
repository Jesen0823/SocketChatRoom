package net.qiujuer.lesson.sample.server;

import net.qiujuer.lesson.sample.server.handle.ClientHandler;
import net.qiujuer.lesson.sample.server.handle.ConnectorStringPacketChain;
import net.qiujuer.library.clink.box.StringReceivePacket;

import java.util.ArrayList;
import java.util.List;

class Group {
    private final String name;
    private final List<ClientHandler> memberList = new ArrayList<>();
    private final GroupMessageAdapter adapter;

    public Group(String name, GroupMessageAdapter adapter) {
        this.name = name;
        this.adapter = adapter;
    }

    public String getName() {
        return name;
    }

    boolean addMember(ClientHandler clientHandler) {
        synchronized (memberList) {
            if (!memberList.contains(clientHandler)) {
                memberList.add(clientHandler);
                clientHandler.getStringPacketChain().appendLast(new ForwardConnectorStringPacketChain());

                System.out.println("Group[" + name + "] add new member: " + clientHandler.getClientInfo());
                return true;
            }
        }
        return false;
    }

    boolean removeMember(ClientHandler clientHandler) {
        synchronized (memberList) {
            if (memberList.remove(clientHandler)) {
                clientHandler.getStringPacketChain().remove(ForwardConnectorStringPacketChain.class);
                System.out.println("Group[" + name + "] leave a member: " + clientHandler.getClientInfo());
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
        protected boolean consume(ClientHandler handler, StringReceivePacket stringReceivePacket) {
            synchronized (memberList) {
                for (ClientHandler member : memberList) {
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
        void sendMessageToClient(ClientHandler handler, String msg);
    }
}
