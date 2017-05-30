package me.t0rr3sp3dr0.if678.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by pedro on 5/18/17.
 */
public final class RTT {
    private RTT() {
        // Avoid class instantiation
    }

    /**
     * Echos all bytes received in the connection
     * Used when calculating RTT
     */
    public static final class Echo extends Thread {
        private final Socket socket;

        public Echo(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            super.run();

            try (final DataInputStream dataInputStream = new DataInputStream(socket.getInputStream())) {
                try (final DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
                    //noinspection InfiniteLoopStatement
                    while (true)
                        dataOutputStream.write(dataInputStream.readByte());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Continuously send a single byte in the connection
     * then waits for the response to calculate RTT
     */
    public static final class Probe extends Thread {
        private final Socket socket;
        private final Callback callback;

        public Probe(Socket socket, Callback callback) {
            this.socket = socket;
            this.callback = callback;
        }

        @Override
        public void run() {
            super.run();

            try (final DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
                try (final DataInputStream dataInputStream = new DataInputStream(socket.getInputStream())) {
                    for (long sequence = 0; sequence < Long.MAX_VALUE; sequence++) {
                        long startTime = System.nanoTime();
                        dataOutputStream.write(new byte[]{0});
                        dataInputStream.readByte();
                        long estimatedTime = System.nanoTime() - startTime;

                        this.callback.onCallback(estimatedTime, sequence);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public interface Callback {
            public abstract void onCallback(long estimatedTime, long sequence);
        }
    }
}
