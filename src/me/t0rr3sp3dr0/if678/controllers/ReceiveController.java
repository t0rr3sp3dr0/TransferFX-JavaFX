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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Created by pedro on 5/4/17.
 */
public class ReceiveController implements Initializable {
    private final Socket socket;

    @FXML
    private Parent root;
    @FXML
    private Label estimatedTimeLabel;
    @FXML
    private Label rttLabel;
    @FXML
    private ProgressBar downloadProgressBar;

    public ReceiveController(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            new RTT.Probe(new Socket(socket.getInetAddress().getHostAddress(), Constants.getPorts()[1]), (estimatedTime, sequence) -> {
                if (sequence % 4096 == 0)
                    Platform.runLater(() -> rttLabel.setText(Long.toString(estimatedTime / 1000)));
            }).start();

            Long contentLength[] = {-1L};
            final File[][] file = {null};

            Thread thread = new Protocol.Receiver(socket, headers -> {
                contentLength[0] = Long.parseLong(headers.get("Content-Length"));

                FileChooser fileChooser = new FileChooser();
                fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
                fileChooser.setInitialFileName(headers.containsKey("Content-Disposition") ? headers.get("Content-Disposition").split("\"")[1] : null);

                Platform.runLater(() -> file[0] = new File[]{fileChooser.showSaveDialog(root.getScene().getWindow())});

                while (file[0] == null)
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                return file[0][0];
            }, (bytesReceived, estimatedTime, sequence) -> {
                if (bytesReceived == contentLength[0]) {
                    Platform.runLater(() -> {
                        estimatedTimeLabel.setText("Transfer Completed");
                        downloadProgressBar.setProgress(1);

                        root.getScene().getWindow().setOnCloseRequest(null);
                    });

                    Utilities.closeCloseables(socket);
                } else if (sequence % 512 == 0) {
                    double speed = bytesReceived / (estimatedTime * Math.pow(10, -9));

                    Platform.runLater(() -> downloadProgressBar.setProgress(bytesReceived / (double) contentLength[0]));
                    if (sequence % 4096 == 0)
                        Platform.runLater(() -> estimatedTimeLabel.setText(String.format("%.2fs\t%.2fMB/s", (contentLength[0] - bytesReceived) / speed, speed / (1024 * 1024))));

                }
            });
            thread.setUncaughtExceptionHandler((t, e) -> {
                e.printStackTrace();

                Platform.runLater(() -> {
                    estimatedTimeLabel.setText("Transfer Failed");
                    downloadProgressBar.setProgress(0);

                    if (file[0] != null && file[0][0] != null)
                        file[0][0].delete();

                    root.getScene().getWindow().setOnCloseRequest(null);
                });

                Utilities.closeCloseables(socket);
            });
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();

            estimatedTimeLabel.setText("Transfer Failed");
            downloadProgressBar.setProgress(0);

            root.getScene().getWindow().setOnCloseRequest(null);

            Utilities.closeCloseables(socket);
        }
    }
}
