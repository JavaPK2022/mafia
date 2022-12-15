package com.example.mafiaclient;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class HelloController {
    /*
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

     */

    @FXML
    private TextArea enterChatTextArea;
    @FXML
    private Button voteButton;
    @FXML
    private VBox playersList;


    private List<DialogPane> playersDialog = new ArrayList<>();
    private int selectedPlayer = -1;


    public void sendChat(KeyEvent event)
    {
        if(event.getCode() == KeyCode.ENTER)
        {
            String text = enterChatTextArea.getText();
            //TODO: change to sending it to the server via Client class
            System.out.println(text.substring(0,text.length()-1));
            enterChatTextArea.clear();
        }
    }

    public void addPlayers(Player player)
    {
        DialogPane pane = new DialogPane();
        Text textHeader = new Text();
        textHeader.setText(player.getNick());
        pane.setHeader(textHeader);
        Text textContent = new Text();
        textContent.setText(player.getRole());
        pane.setContent(textContent);
        pane.setId(String.valueOf(playersDialog.size()));
        pane.setOnMouseClicked(event -> {
            System.out.println(player.getID());
            if(selectedPlayer<0) {
                pane.setBackground(new Background(
                        new BackgroundFill(Color.web("#d45148"), CornerRadii.EMPTY, Insets.EMPTY)));
                selectedPlayer = Integer.parseInt(pane.getId());
            }else if(selectedPlayer == Integer.parseInt(pane.getId()))
            {
                pane.setBackground(new Background(
                        new BackgroundFill(Color.web("#ffffff"), CornerRadii.EMPTY, Insets.EMPTY)));
                selectedPlayer = -1;
            }else
            {
                pane.setBackground(new Background(
                        new BackgroundFill(Color.web("#d45148"), CornerRadii.EMPTY, Insets.EMPTY)));
                DialogPane selectedPane = playersDialog.get(selectedPlayer);
                selectedPane.setBackground(new Background(
                        new BackgroundFill(Color.web("#ffffff"), CornerRadii.EMPTY, Insets.EMPTY)));
                selectedPlayer = Integer.parseInt(pane.getId());
            }
        });

        playersDialog.add(pane);

        playersList.getChildren().add(pane);

    }

    public void testAddPlayer()
    {
        addPlayers(new Player(1,"nick","role"));
    }

}