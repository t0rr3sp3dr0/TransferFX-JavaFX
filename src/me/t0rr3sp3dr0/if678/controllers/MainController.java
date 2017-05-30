package me.t0rr3sp3dr0.if678.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import me.t0rr3sp3dr0.if678.util.Constants;
import me.t0rr3sp3dr0.if678.util.Utilities;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML
    public TextField textField;
    @FXML
    public Button button;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        textField.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.ENTER))
                button.fire();
        });

        // Starts a new connection with indicated host
        // Then it will open a window for connection interaction
        button.setOnAction(event -> {
            try {
                final Socket socket = new Socket(textField.getText(), Constants.getPorts()[0]);

                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/layouts/send.fxml"));
                fxmlLoader.setController(new SendController(socket));

                Parent root = fxmlLoader.load();

                Stage stage = new Stage();
                stage.setOnCloseRequest(Utilities.closeCloseablesOnAbortRequest(socket));
                stage.setTitle(socket.getRemoteSocketAddress().toString());
                stage.setScene(new Scene(root, 340, 68));
                stage.show();

                textField.setText(null);
                textField.requestFocus();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
