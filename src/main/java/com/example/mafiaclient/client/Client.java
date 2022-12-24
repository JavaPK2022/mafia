package com.example.mafiaclient.client;


import com.example.mafiaclient.HelloController;
import javafx.application.Platform;

import java.io.*;
import java.net.*;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private InetAddress ipAddress;
    private WaitForServer waitForServer = new WaitForServer();
    private DatagramSocket datagramSocket = new DatagramSocket();
    private PlayerChat playerChat;
    private Player player;
    private MulticastSocket multicastSocket = new MulticastSocket(4442);

    public Client(String ip, int port, HelloController controller, Player player) throws IOException {
        ipAddress = InetAddress.getByName("localhost");
        multicastSocket.joinGroup(InetAddress.getByName("230.0.0.0"));
        this.player = player;
        playerChat = new PlayerChat(controller);
        playerChat.start();
        System.out.println("client start");
        InitializationThread initializationThread = new InitializationThread(controller);
        initializationThread.start();
        startConnection(ip, port);

    }

    public void startConnection(String ip, int port) throws IOException {
        System.out.println(player.toString());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(player);
        oos.close();
        byte[] buf = baos.toByteArray();
        DatagramPacket packet = new DatagramPacket(buf,buf.length,ipAddress,4446);
        datagramSocket.send(packet);
        socket = new Socket(ip, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        waitForServer.start();
    }

    public void sendMessageToChat(String msg) throws IOException {
        playerChat.sendMessageToChat(msg);
        /*
        System.out.println("message "+msg);
        byte[] buf = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buf,buf.length, ipAddress,4444);
        //System.out.println("message "+msg);
        datagramSocket.send(packet);

         */
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        socket.close();
    }

    public void sendVote(int playerId)
    {
        out.println(playerId);
    }

    private class WaitForServer extends Thread{
        @Override
        public void run()
        {
            //server messages are strings starting with 2 number specifying the type of message
            try {
                String message = in.readLine();
                String messageType = message.substring(0, 2);

                //TODO: add more cases
                switch (messageType){
                    case "01":
                        updateChat(message.substring(2));
                        break;
                     default:
                        break;
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void updateChat(String message)
        {
            System.out.println(message);
        }
    }

    private class InitializationThread extends Thread{

        private HelloController controller;

        public InitializationThread(HelloController controller)
        {
            this.controller = controller;
        }
        @Override
        public void run()
        {
            while (true)
            {
                byte[] buf = new byte[512];
                DatagramPacket packet = new DatagramPacket(buf,buf.length);
                try {
                    multicastSocket.receive(packet);
                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Player player = (Player) ois.readObject();
                    ois.close();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            controller.addPlayers(player);
                        }
                    });

                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }

}
