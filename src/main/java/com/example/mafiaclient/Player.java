package com.example.mafiaclient;

import java.io.Serializable;

public class Player implements Serializable {
    private int ID;
    private String role;
    private String nick;

    public Player(int ID, String role, String nick) {
        this.ID = ID;
        this.role = role;
        this.nick = nick;
    }

    public int getID() {
        return ID;
    }

    public String getRole() {
        return role;
    }

    public String getNick() {
        return nick;
    }
}
