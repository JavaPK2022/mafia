package com.example.mafiaclient;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private WaitForServer waitForServer = new WaitForServer();

    public void startConnection(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        waitForServer.start();
    }

    public void sendMessageToChat(String msg) throws IOException {
        out.println(msg);
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

}
