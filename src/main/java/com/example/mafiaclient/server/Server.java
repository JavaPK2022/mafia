package com.example.mafiaclient.server;

import com.example.mafiaclient.client.Player;
import com.example.mafiaclient.client.RoleEnum;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {

    private final List<Player> playersList = Collections.synchronizedList(new ArrayList<Player>());
    private final List<ServerThread> serverThreadList = Collections.synchronizedList(new ArrayList<ServerThread>());
    private InetAddress group;
    private final DatagramSocket datagramSocket = new DatagramSocket(4446);
    private final ServerSocket socket = new ServerSocket(4445);
    private AtomicBoolean gameStarted = new AtomicBoolean(false);
    private int playerCount = 0;
    private final GameState gameState = new GameState();


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
                        continue;
                    }
                    addNewServerThread(clientSocket);

                    Player player = getNewPlayer(packet);

                    addNewPlayer(player);


                    System.out.println("player ID is "+player.getID());

                    sendNewPlayer(player);
                    //PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    //out.println("06");
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }

        private void sendNewPlayer(Player player) throws IOException {
            DatagramPacket packet;
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
            objectStream.writeObject(player);

            byte[] bufSend = byteStream.toByteArray();
            packet = new DatagramPacket(bufSend, bufSend.length,group,4442);
            datagramSocket.send(packet);
        }

        private void addNewPlayer(Player player) {
            playerCount++;
            playersList.add(player);
        }

        private Player getNewPlayer(DatagramPacket packet) throws IOException, ClassNotFoundException {
            ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
            ObjectInputStream ois = new ObjectInputStream(bais);
            Player player = (Player) ois.readObject();
            ois.close();
            System.out.println(player.toString());
            player.setID(playerCount);
            return player;
        }

        private void addNewServerThread(Socket clientSocket) throws IOException {
            ServerThread serverThread = new ServerThread(clientSocket);
            serverThreadList.add(serverThread);
            serverThread.start();
        }


    }

    private void gameAlreadyHasStartedException(Socket socket) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out.println("04GameAlreadyHasStarted");
    }

    private void drawPlayers()
    {
        int numberOfMafiaPlayers = (int) Math.ceil(0.3*playerCount);
        Random random = new Random();
        List<RoleEnum> roleList = new ArrayList<>();
        for(int i = 0; i<playerCount; i++)
        {
            roleList.add(RoleEnum.NOT_INITIALIZED);
        }
        for(int i = 0; i<numberOfMafiaPlayers; i++)
        {
            int x = random.nextInt(playerCount);
            if(roleList.get(x) == RoleEnum.NOT_INITIALIZED)
                roleList.set(x, RoleEnum.MAFIA);
            else
                i--;
        }
        int x = random.nextInt(playerCount);
        while (roleList.get(x)!=RoleEnum.NOT_INITIALIZED)
            x = random.nextInt(playerCount);

        roleList.set(x,RoleEnum.DETECTIVE);

        for(int i = 0; i< playerCount; i++)
        {
            if(roleList.get(i)==RoleEnum.NOT_INITIALIZED)
                roleList.set(i, RoleEnum.REGULAR);
        }

        List<Player> staticPlayerList = new ArrayList<>();

        synchronized (playersList)
        {
            for (Player player : playersList) {
                player.setRole(roleList.get(player.getID()));
                staticPlayerList.add(player);
            }
        }

        synchronized (serverThreadList)
        {
            for (ServerThread serverThread : serverThreadList) {
                for (int i = 0; i < playerCount; i++) {
                    serverThread.sendPlayerUpdate(staticPlayerList.get(i));
                }
                serverThread.finishPlayerUpdate();

            }
        }


    }

    private class ServerThread extends Thread{

        private final PrintWriter out;// = new PrintWriter(clientSocket.getOutputStream(), true);
        private final BufferedReader in;// = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        private Socket clientSocket;

        public ServerThread(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }
        @Override
        public void run()
        {
            ByteArrayOutputStream gameStateOutputStreamBytes = new ByteArrayOutputStream();

            synchronized (playersList) { //TODO to do rozsy??ania
                int size = 0;
                for (Player value : playersList) {
                    size++;
                    Player player = value;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    try {
                        sendPlayer(player, baos);

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }


                }

                out.println("05"+size);
                try {
                    sendGameState(gameStateOutputStreamBytes);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


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
                            drawPlayers();
                            break;
                        default:
                            break;
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        private void sendPlayer(Player player, ByteArrayOutputStream baos) throws IOException {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(player);
            byte[] serializedPlayer = baos.toByteArray();
            System.out.println(String.valueOf("decoder "+Base64.getEncoder().encodeToString(serializedPlayer)));
            String serializedObject = baos.toString();
            System.out.println("string "+serializedObject);
            System.out.println("(sthread " + this.getName() +") Server sends player with ID "+ player.getID());
            serializedObject = String.valueOf("02" + Base64.getEncoder().encodeToString(serializedPlayer));
            out.println(serializedObject);
        }

        private void sendGameState(ByteArrayOutputStream gameStateOutputStreamBytes) throws IOException {
            System.out.println("send state");
            ObjectOutputStream gameStateOutputStreamObject = new ObjectOutputStream(gameStateOutputStreamBytes);
            gameStateOutputStreamObject.writeObject(gameState);
            byte[] serializedGameState = gameStateOutputStreamBytes.toByteArray();
            String serializedObject = String.valueOf("08" + Base64.getEncoder().encodeToString(serializedGameState));
            out.println(serializedObject);
        }

        public void sendPlayerUpdate(Player player)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(player);

                byte[] serializedPlayer = baos.toByteArray();
                String serializedObject;// = baos.toString();
                serializedObject = String.valueOf("06" + Base64.getEncoder().encodeToString(serializedPlayer));
                out.println(serializedObject);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void finishPlayerUpdate()
        {
            out.println("07Finish");
        }
    }
}
