package com.example.mafiaclient.client;

import com.example.mafiaclient.HelloController;
import javafx.application.Platform;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class PlayerChat extends Thread{

    private MulticastSocket multicastSocket = new MulticastSocket(4443);
    private DatagramSocket datagramSocket = new DatagramSocket();
    private InetAddress ipAddress = InetAddress.getByName("localhost");
    private HelloController controller;


    public PlayerChat(HelloController controller) throws IOException {
        multicastSocket.joinGroup(InetAddress.getByName("230.0.0.0"));
        this.controller = controller;

    }

    public void sendMessageToChat(String msg) throws IOException {
        System.out.println("message "+msg);
        byte[] buf = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buf,buf.length, ipAddress,4444);
        //System.out.println("message "+msg);
        datagramSocket.send(packet);
    }

    @Override
    public void run() {
        while (true)
        {
            byte[] buf = new byte[512];
            DatagramPacket packet = new DatagramPacket(buf,buf.length);
            try {
                multicastSocket.receive(packet);
                String received = new String(packet.getData(),0,packet.getLength());
                System.out.println(received+" it works");
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        controller.updateChat(received);
                    }
                });

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
