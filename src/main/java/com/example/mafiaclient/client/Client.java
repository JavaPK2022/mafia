package com.example.mafiaclient.client;


import com.example.mafiaclient.HelloController;
import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.Base64;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private InetAddress ipAddress;
    private WaitForServer waitForServer;// = new WaitForServer();
    private DatagramSocket datagramSocket = new DatagramSocket();
    private PlayerChat playerChat;
    private Player player;
    private MulticastSocket multicastSocket = new MulticastSocket(4442);
    private InitializationThread initializationThread;

    public Client(String ip, int port, HelloController controller, Player player) throws IOException {
        ipAddress = InetAddress.getByName("localhost");
        multicastSocket.joinGroup(InetAddress.getByName("230.0.0.0"));
        waitForServer = new WaitForServer(controller);
        this.player = player;
        playerChat = new PlayerChat(controller);
        playerChat.start();
        System.out.println("client start");
        initializationThread = new InitializationThread(controller);
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
        if(!playerChat.isInterrupted())
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
        playerChat.interrupt();
        multicastSocket.close();
        datagramSocket.close();
    }

    public void sendVoteOrStartGame(int playerId)
    {
        //out.println(playerId);
        if(playerId<0)
        {
            out.println("01StartGame");
        }
    }

    private class WaitForServer extends Thread{

        private HelloController controller;

        public WaitForServer(HelloController controller) {
            this.controller = controller;
        }

        @Override
        public void run() {
            while (true) {
                //server messages are strings starting with 2 number specifying the type of message
                try {
                    String message = in.readLine();
                    String messageType = message.substring(0, 2);
                    String onlyMessage = message.substring(2);
                    System.out.println(onlyMessage);

                    //TODO: add more cases
                    switch (messageType) {
                        case "01":
                            updateChat(message.substring(2));
                            break;
                        case "02":
                            byte[] bytes = Base64.getDecoder().decode(onlyMessage);
                            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                            ObjectInputStream ois = new ObjectInputStream(bais);
                            Player receivedPlayer = (Player) ois.readObject();
                            ois.close();
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    controller.addPlayers(receivedPlayer);
                                }
                            });
                            break;
                        case "03":
                            Platform.runLater(new Runnable() {
                                                  @Override
                               public void run() {
                                    controller.setHost();
                               }
                               }
                            );
                            break;
                        case "04":
                            playerChat.interrupt();
                            initializationThread.interrupt();
                            stopConnection();
                            System.out.println("game already has started");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    controller.exitGameAlert();
                                }
                            });
                            return;
                        case "05":
                            player.setID(Integer.parseInt(onlyMessage));
                            System.out.println("My new ID is "+onlyMessage);
                            break;
                        default:
                            break;
                    }

                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
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

                }catch (InterruptedIOException e){
                    return;
                }
                catch (IOException | ClassNotFoundException e) {
                    if(this.isInterrupted())
                        System.out.println("User tried to connect to the closed game");
                    throw new RuntimeException(e);
                }

            }
        }
    }

}
