package com.example.mafiaclient.server;

import java.net.SocketException;
import java.net.UnknownHostException;

public class Server {

    public Server() throws SocketException, UnknownHostException {
        ChatThread chatThread = new ChatThread();
        chatThread.start();
    }
}
