package org.example.threads;

import org.example.player.Player;
import org.example.session.Session;
import org.example.session.Turn;

import java.util.concurrent.Callable;

public class TurnReceiverThread implements Callable<Turn> {
    private Session sessionState;
    private Player connectedPlayer;


    public Turn call() throws Exception {
        //Wysyła stan rozgrywki (TODO musi być okrojony)
        //czeka na wykonaną ture gracza, po czym wysyła jej wynik
        return null;
    }
}
