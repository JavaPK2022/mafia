package com.example.mafiaclient.client;

public class PlayerAction {
    private int clientId;
    private int targetId;

    public PlayerAction(int clientId, int targetId) {
        this.clientId = clientId;
        this.targetId = targetId;
    }

    public int getClientId() {
        return clientId;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }
}

