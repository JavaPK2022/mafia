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
    private int playersVoted = 0;
    private int mafiaCounter = 0;
    private Map<Integer, Integer> playerVoteMap = new HashMap<>();
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

    private void drawPlayers(ByteArrayOutputStream gameStateOutputStreamBytes) throws IOException {
        int numberOfMafiaPlayers = (int) Math.ceil(0.3*playerCount);
        mafiaCounter = numberOfMafiaPlayers;
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
                    //ByteArrayOutputStream gameStateOutputStreamBytes = new ByteArrayOutputStream();
                    //serverThread.sendGameState(gameStateOutputStreamBytes);
                }
                serverThread.finishPlayerUpdate();
                serverThread.sendGameState(gameStateOutputStreamBytes);
                //todo tutaj wysłać gamestate?
            }
        }


    }

    public void updateVotedPlayers(ByteArrayOutputStream gameStateOutputStreamBytes) throws IOException {

        Optional<Map.Entry<Integer, Integer>> max = playerVoteMap.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        Player playerToRemove = playersList.stream()
                .filter(player -> player.getID() == max.get().getKey())
                .findAny()
                .orElse(null);
        //usuwamy gracza i zerujemy odpowiednie pola

        if(playerToRemove.getRole()==RoleEnum.MAFIA)
            mafiaCounter--;

        playerCount--;
        playersVoted = 0;
        playerVoteMap = new HashMap<>();
        playerToRemove.setRole(RoleEnum.DECEASED);
        playersList.remove(playerToRemove);
        System.out.println("deleted player's id: "+playerToRemove.getID() +" "+playerToRemove.getNick());
        System.out.println("game state1 "+gameState.isNight());
        gameState.toggleState();
        System.out.println("game state2 "+gameState.isNight());
        synchronized (serverThreadList)
        {
            for (ServerThread thread: serverThreadList)
            {
                //sprawdzamy kto ma najwiecej głosów

                thread.checkForGameEnd();

                //wysyłamy dane na temat stanu gry i martwego gracza
                gameStateOutputStreamBytes = new ByteArrayOutputStream();
                thread.sendPlayerUpdate2(playerToRemove);
                thread.sendGameState(gameStateOutputStreamBytes);
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

            synchronized (playersList) { //TODO to do rozsyłania
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
                            drawPlayers(gameStateOutputStreamBytes);

                            break;
                        case "02":
                            String[] messageParts = messageFromClient.split(" ");
                            //sprawdzamy czy na pewno wartość z vote'a jest w aktywnych graczach
                            if (playersList.stream()
                                    .map(Player::getID).toList().contains(Integer.valueOf(messageParts[2]))) {
                                //jeśli nikt nie zagłosował jeszcze na gracza z danym ID to ustawiamy głos na jeden
                                //jeśli ktoś już wcześniej zagłosował to zwiększamy liczbę głosów o 1
                                playerVoteMap.merge( Integer.valueOf(messageParts[2]), 1, Integer::sum);
                                playersVoted++;
                                //jeśli liczba graczy którzy zagłosowali jest równa liczbie graczy dalej w grze
                                //odrzucimy gracza z najweikszą liczbą głosów
                                if ((playersVoted == playersList.size()) ||(gameState.isNight() && playersVoted == mafiaCounter)) {
                                   updateVotedPlayers(gameStateOutputStreamBytes);
                                }
                            }
                        default:
                            break;
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        private void checkForGameEnd() {
            //TODO: zhandlowanie końca gry
            //jeśli żaden gracz z mafii nie żyje to zrób coś
            /*long mafiaAlivePlayers = playersList.stream()
                    .map(Player::getRole)
                    .filter(role -> role.equals(RoleEnum.MAFIA)).count();

             */
            if (mafiaCounter == 0) {
                System.out.println("mafia lost server");
                out.println("10false");
            }
            //jak wyżej
            /*long townAlivePlayers = playersList.stream()
                    .map(Player::getRole)
                    .filter(role -> role.equals(RoleEnum.DETECTIVE) || role.equals(RoleEnum.REGULAR)).count();

             */
            if (playerCount - mafiaCounter <= 1) {
                System.out.println("mafia won server");
                out.println("10true");
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

        public void sendPlayerUpdate2(Player player)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(player);

                byte[] serializedPlayer = baos.toByteArray();
                String serializedObject;// = baos.toString();
                serializedObject = String.valueOf("09" + Base64.getEncoder().encodeToString(serializedPlayer));
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
