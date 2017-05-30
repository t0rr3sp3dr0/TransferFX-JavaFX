package me.t0rr3sp3dr0.if678.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import me.t0rr3sp3dr0.if678.util.Constants;
import me.t0rr3sp3dr0.if678.util.Protocol;
import me.t0rr3sp3dr0.if678.util.RTT;
import me.t0rr3sp3dr0.if678.util.Utilities;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by pedro on 5/4/17.
 */
public class SendController implements Initializable {
    private final Socket socket;

    @FXML
    private Parent root;
    @FXML
    private Label estimatedTimeLabel;
    @FXML
    private Label rttLabel;
    @FXML
    private ProgressBar uploadProgressBar;
    @FXML
    private Button uploadButton;

    public SendController(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            new RTT.Probe(new Socket(socket.getInetAddress().getHostAddress(), Constants.getPorts()[1]), (estimatedTime, sequence) -> {
                if (sequence % 4096 == 0)
                    Platform.runLater(() -> rttLabel.setText(Long.toString(estimatedTime / 1000)));
            }).start();

            uploadButton.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                final File file = fileChooser.showOpenDialog(root.getScene().getWindow());
                if (file != null) {
                    estimatedTimeLabel.setText("Waiting");
                    uploadButton.setDisable(true);

                    final long finalLength = file.length();

                    Thread thread = new Protocol.Sender(socket, file, (bytesSent, estimatedTime, sequence) -> {
                        if (bytesSent == finalLength) {
                            Platform.runLater(() -> {
                                estimatedTimeLabel.setText("Transfer Completed");
                                uploadProgressBar.setProgress(1);

                                root.getScene().getWindow().setOnCloseRequest(null);
                            });

                            Utilities.closeCloseables(socket);
                        } else if (sequence % 512 == 0) {
                            double speed = bytesSent / (estimatedTime * Math.pow(10, -9));

                            Platform.runLater(() -> uploadProgressBar.setProgress(bytesSent / (double) finalLength));
                            if (sequence % 4096 == 0)
                                Platform.runLater(() -> estimatedTimeLabel.setText(String.format("%.2fs\t%.2fMB/s", (finalLength - bytesSent) / speed, speed / (1024 * 1024))));

                        }
                    });
                    thread.setUncaughtExceptionHandler((t, e) -> {
                        e.printStackTrace();

                        Platform.runLater(() -> {
                            estimatedTimeLabel.setText("Transfer Failed");
                            uploadProgressBar.setProgress(0);
                            uploadButton.setDisable(true);

                            root.getScene().getWindow().setOnCloseRequest(null);
                        });

                        Utilities.closeCloseables(socket);
                    });
                    thread.start();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();

            estimatedTimeLabel.setText("Transfer Failed");
            uploadProgressBar.setProgress(0);
            uploadButton.setDisable(true);

            root.getScene().getWindow().setOnCloseRequest(null);

            Utilities.closeCloseables(socket);
        }
    }
}
