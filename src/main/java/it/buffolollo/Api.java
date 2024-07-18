package it.buffolollo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class Api {
    private ServerSocket serverSocket;
    private Map<String, BiConsumer<Request, Response>> getHandlers = new HashMap<>();
    private Map<String, BiConsumer<Request, Response>> postHandlers = new HashMap<>();

    public Api(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void get(String path, BiConsumer<Request, Response> handler) {
        getHandlers.put(path, handler);
    }

    public void post(String path, BiConsumer<Request, Response> handler) {
        postHandlers.put(path, handler);
    }

    public void listen(Runnable callback) {
        try {
            callback.run();

            while (true) {
                Socket clientSocket = serverSocket.accept();

                new Thread(new ClientHandler(clientSocket, getHandlers, postHandlers)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
