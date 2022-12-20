package com.example.mafiaclient.client;

import java.io.IOException;

public class ClientMain {

    public static void main(String[] args) throws IOException {
        Client client = new Client("127.0.0.1",4445);
        client.sendMessageToChat("01test");
    }
}
