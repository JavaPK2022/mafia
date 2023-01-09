package com.example.mafiaclient.server;

import java.io.Serializable;

public class GameState implements Serializable {
    //TODO
    private boolean isNight =true;

    public void toggleState(){
        isNight=!isNight;
    }
    public boolean isNight(){
        return isNight;
    }

}
