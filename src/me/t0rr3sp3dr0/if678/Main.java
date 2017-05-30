package me.t0rr3sp3dr0.if678;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import me.t0rr3sp3dr0.if678.controllers.ReceiveController;
import me.t0rr3sp3dr0.if678.util.Constants;
import me.t0rr3sp3dr0.if678.util.RTT;
import me.t0rr3sp3dr0.if678.util.Utilities;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

public class Main extends Application {
    private static final boolean developerMode = true;
    private static final boolean waitOnExcept = true;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if (developerMode)
                e.printStackTrace();

            Utilities.alertException((Exception) e, waitOnExcept);
        });

        primaryStage.setOnCloseRequest(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(null);
            alert.setHeaderText("Are you sure you want to exit?");
            alert.setContentText(null);

            Button exitButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
            exitButton.setText("Exit");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK)
                System.exit(0);
            else
                event.consume();
        });

        Parent root = FXMLLoader.load(getClass().getResource("/layouts/main.fxml"));
        primaryStage.setTitle("TransferFX");
        primaryStage.setScene(new Scene(root, 340, 68));
        primaryStage.show();

        // Starts application server
        // For each client connected, a new window will open for interaction with that client
        new Thread() {
            @Override
            public void run() {
                super.run();

                try (final ServerSocket serverSocket = new ServerSocket(Constants.getPorts()[0])) {
                    //noinspection InfiniteLoopStatement
                    while (true) {
                        final Socket socket = serverSocket.accept();

                        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/layouts/receive.fxml"));
                        fxmlLoader.setController(new ReceiveController(socket));

                        final Parent root = fxmlLoader.load();

                        Platform.runLater(() -> {
                            Stage stage = new Stage();
                            stage.setOnCloseRequest(Utilities.closeCloseablesOnAbortRequest(socket));
                            stage.setTitle(socket.getRemoteSocketAddress().toString());
                            stage.setScene(new Scene(root, 340, 68));
                            stage.show();
                        });
                    }
                } catch (final IOException e) {
                    e.printStackTrace();

                    Platform.runLater(() -> {
                        Utilities.alertException(e, true);

                        System.exit(1);
                    });
                }
            }
        }.start();

        // Starts echo server (used when calculating RTT)
        new Thread() {
            @Override
            public void run() {
                super.run();

                try (final ServerSocket serverSocket = new ServerSocket(Constants.getPorts()[1])) {
                    //noinspection InfiniteLoopStatement
                    while (true)
                        new RTT.Echo(serverSocket.accept()).start();
                } catch (final IOException e) {
                    e.printStackTrace();

                    Platform.runLater(() -> {
                        Utilities.alertException(e, true);

                        System.exit(1);
                    });
                }
            }
        }.start();
    }

    public static boolean isDeveloperMode() {
        return developerMode;
    }

    public static boolean isWaitOnExcept() {
        return waitOnExcept;
    }
}
