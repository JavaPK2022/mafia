package com.example.mafiaclient.client;

import java.io.Serializable;

public class Player implements Serializable {
    private int ID;
    private RoleEnum role;
    private String nick;

    public Player(int ID, RoleEnum role, String nick) {
        this.ID = ID;
        this.role = role;
        this.nick = nick;
    }

    public int getID() {
        return ID;
    }

    public RoleEnum getRole() {
        return role;
    }

    public String getNick() {
        return nick;
    }
}
