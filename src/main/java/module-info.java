module com.example.mafiaclient {
    requires javafx.controls;
    requires javafx.fxml;
    //requires jfxrt;
    //requires rt;


    opens com.example.mafiaclient to javafx.fxml;
    exports com.example.mafiaclient;
    exports com.example.mafiaclient.client;
    opens com.example.mafiaclient.client to javafx.fxml;
    exports com.example.mafiaclient.server;
    opens com.example.mafiaclient.server to javafx.fxml;
}