package com.example.mafiaclient.server;

import java.io.IOException;
import java.net.*;

public class ChatThread extends Thread{

    private DatagramSocket socket = new DatagramSocket(4444);
    private InetAddress group;
    private byte[] buf;


    public ChatThread() throws SocketException, UnknownHostException {
        group = InetAddress.getByName("230.0.0.0");
    }

    @Override
    public void run() {
        DatagramPacket packet;
        System.out.println("chat thread start");
        while(true) {
            try {
                buf = new byte[512];
                packet =  new DatagramPacket(buf,buf.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println(received);
                if (received.substring(0, 2).equals("01")) {
                    received = received.substring(2);
                    buf = received.getBytes();
                    packet = new DatagramPacket(buf, buf.length, group, 4443);
                    System.out.println("send chat message");
                    socket.send(packet);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
