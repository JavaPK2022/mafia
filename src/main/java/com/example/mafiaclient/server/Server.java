package com.example.mafiaclient.server;

import com.example.mafiaclient.client.Player;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server {

    private List<Player> playersList = new ArrayList<>();
    private InetAddress group;
    private DatagramSocket datagramSocket = new DatagramSocket(4446);
    private ServerSocket socket = new ServerSocket(4445);

    public Server() throws IOException {
        group = InetAddress.getByName("230.0.0.0");
        ChatThread chatThread = new ChatThread();
        chatThread.start();
        InitializationThread initializationThread = new InitializationThread();
        initializationThread.start();
    }


    private class InitializationThread extends Thread
    {
        @Override
        public void run()
        {
            while(true)
            {
                byte[] buf = new byte[512];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    datagramSocket.receive(packet);
                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Player player = (Player) ois.readObject();
                    ois.close();
                    System.out.println(player.toString());
                    playersList.add(player);
                    byte[] bufSend = packet.getData();
                    packet = new DatagramPacket(bufSend, bufSend.length,group,4442);
                    datagramSocket.send(packet);
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
