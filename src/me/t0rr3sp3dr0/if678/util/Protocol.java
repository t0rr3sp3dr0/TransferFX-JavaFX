package me.t0rr3sp3dr0.if678.util;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import javafx.application.Platform;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pedro on 5/21/17.
 */
public final class Protocol {
    /**
     * This protocol was based on HTTP protocol
     * The protocol is divided in tow sections: headers and body
     * Those two sections are delimited by a single blank line between them
     * Headers are treated just like in HTTP
     * Body differs from HTTP by accepting binary content
     */
    private Protocol() {
        // Avoid class instantiation
    }

    /**
     * Sends file through connection
     * Firstly it sends, via headers, file's length and name
     * Then it streams the file in blocks of MTU size
     * Finally it verifies if the hole file has been transmitted
     */
    public static final class Sender extends Thread {
        private final Socket socket;
        private final File file;
        private final Callback callback;

        public Sender(@NotNull Socket socket, @NotNull File file, @Nullable Callback callback) {
            this.socket = socket;
            this.file = file;
            this.callback = callback;
        }

        @Override
        public void run() {
            super.run();

            try (OutputStream outputStream = socket.getOutputStream()) {
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    byte[] bytes = new byte[Constants.getMTU()];

                    outputStream.write(String.format("Content-Length: %d\n", file.length()).getBytes());
                    outputStream.write(String.format("Content-Disposition: attachment; filename=\"%s\"\n", file.getName()).getBytes());
                    outputStream.write("\n".getBytes());

                    long writtenSize = 0;
                    int count = 0;
                    int size;
                    long startTime = System.nanoTime();
                    while ((size = fileInputStream.read(bytes)) > 0) {
                        outputStream.write(bytes, 0, size);
                        long estimatedTime = System.nanoTime() - startTime;
                        writtenSize += size;
                        count++;

                        if (callback != null)
                            callback.onCallback(writtenSize, estimatedTime, count);
                    }

                    if (writtenSize != file.length())
                        throw new RuntimeException("Content Length Mismatch");

                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();

                throw new RuntimeException(e);
            }
        }

        public interface Callback {
            public abstract void onCallback(double bytesSent, long estimatedTime, long sequence);
        }
    }

    /**
     * Receives file in connection
     * Firstly it parses the headers of the stream
     * Then it reads the file from stream with a buffer of MTU size
     * Finally it verifies if the hole file has been recovered
     */
    public static final class Receiver extends Thread {
        private final Socket socket;
        private final FileRequestHandler fileRequestHandler;
        private final Callback callback;

        public Receiver(@NotNull Socket socket, @NotNull FileRequestHandler fileRequestHandler, @Nullable Callback callback) {
            this.socket = socket;
            this.fileRequestHandler = fileRequestHandler;
            this.callback = callback;
        }

        @Override
        public void run() {
            super.run();

            try (InputStream inputStream = socket.getInputStream()) {
                final Map<String, String> headers = getHeaders(inputStream);
                final File file = fileRequestHandler.onFileRequest(headers);

                final byte[] bytes = new byte[Constants.getMTU()];
                Long contentLength = Long.parseLong(headers.get("Content-Length"));

                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    long readSize = 0;
                    int count = 0;
                    long startTime = System.nanoTime();
                    while (readSize < contentLength) {
                        int size = inputStream.read(bytes);
                        if (size == 0)
                            continue;

                        fileOutputStream.write(bytes, 0, size);
                        long estimatedTime = System.nanoTime() - startTime;
                        readSize += size;
                        count++;

                        if (callback != null)
                            callback.onCallback(readSize, estimatedTime, count);
                    }

                    if (readSize != file.length())
                        throw new RuntimeException("Content Length Mismatch");
                }
            } catch (IOException e) {
                e.printStackTrace();

                throw new RuntimeException(e);
            }
        }

        public interface FileRequestHandler {
            public abstract @NotNull
            File onFileRequest(Map<String, String> headers);
        }

        public interface Callback {
            public abstract void onCallback(double bytesReceived, long estimatedTime, long sequence);
        }
    }

    public static Map<String, String> getHeaders(InputStream inputStream) throws IOException {
        final byte[] bytes = new byte[1];
        final Map<String, String> headers = new HashMap<>();

        StringBuilder headerBuffer = new StringBuilder();
        while (inputStream.read(bytes) > 0)
            if (bytes[0] == '\n' && headerBuffer.charAt(headerBuffer.length() - 1) == '\n')
                break;
            else
                headerBuffer.append((char) bytes[0]);

        String rawHeaders = headerBuffer.toString().trim();
        System.err.println(rawHeaders);

        for (String header : rawHeaders.split("\n")) {
            String[] strings = header.split(": ");
            if (strings.length == 2)
                headers.put(strings[0], strings[1]);
        }

        return headers;
    }
}
