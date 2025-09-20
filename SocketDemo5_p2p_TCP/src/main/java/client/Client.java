package client;

import client.bean.ServerInfo;

import java.io.IOException;

public class Client {
    public static void main(String[] args) throws IOException {
        ServerInfo info = ClientSearcher.searchServer(10000);
        System.out.println("[C] Client: "+info);
        if (info!=null){
            TCPClient.linkWith(info);
        }
    }
}
