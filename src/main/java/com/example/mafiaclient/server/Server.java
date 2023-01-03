package com.example.mafiaclient.server;

import com.example.mafiaclient.client.Player;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {

    private List<Player> playersList = Collections.synchronizedList(new ArrayList<Player>());
    private InetAddress group;
    private DatagramSocket datagramSocket = new DatagramSocket(4446);
    private ServerSocket socket = new ServerSocket(4445);
    private AtomicBoolean gameStarted = new AtomicBoolean(false);
    private int playerCount = 0;


    public Server() throws IOException {
        group = InetAddress.getByName("230.0.0.0");
        ChatThread chatThread = new ChatThread();
        chatThread.start();
        InitializationThread initializationThread = new InitializationThread();
        initializationThread.start();
    }


    //waits for new players
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
                    Socket clientSocket = socket.accept();
                    if(gameStarted.get())
                    {
                        gameAlreadyHasStartedException(clientSocket);
                        break;
                    }
                    ServerThread serverThread = new ServerThread(clientSocket);
                    serverThread.start();
                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Player player = (Player) ois.readObject();
                    ois.close();
                    System.out.println(player.toString());
                    player.setID(playerCount);
                    playerCount++;
                    playersList.add(player);
                    System.out.println("player ID is "+player.getID());
                    byte[] bufSend = packet.getData();
                    packet = new DatagramPacket(bufSend, bufSend.length,group,4442);
                    datagramSocket.send(packet);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void gameAlreadyHasStartedException(Socket socket) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out.println("04GameAlreadyHasStarted");
    }

    private class ServerThread extends Thread{

        private PrintWriter out;// = new PrintWriter(clientSocket.getOutputStream(), true);
        private BufferedReader in;// = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        private Socket clientSocket;

        public ServerThread(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }
        @Override
        public void run()
        {
            synchronized (playersList) {
                int size = 0;
                Iterator<Player> iterator = playersList.iterator();
                while(iterator.hasNext()) {
                    size++;
                    Player player = iterator.next();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(player);
                        byte[] serializedPlayer = baos.toByteArray();
                        System.out.println(String.valueOf("decoder "+Base64.getEncoder().encodeToString(serializedPlayer)));
                        String serializedObject = baos.toString();
                        System.out.println("string "+serializedObject);
                        serializedObject = String.valueOf("02" + Base64.getEncoder().encodeToString(serializedPlayer));
                        out.println(serializedObject);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }

                out.println("05"+size);


                if(size==0)
                {
                    out.println("03fistPlayer");
                    System.out.println("fist player");
                }
            }

            while (true)
            {
                try {
                    String messageFromClient = in.readLine();
                    String messageType = messageFromClient.substring(0,2);
                    switch (messageType)
                    {
                        case "01":
                            gameStarted.set(true);
                            break;
                        default:
                            break;
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }
}
