module com.example.mafiaclient {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.mafiaclient to javafx.fxml;
    exports com.example.mafiaclient;
}