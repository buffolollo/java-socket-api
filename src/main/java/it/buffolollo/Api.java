package it.buffolollo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The Api class represents a simple HTTP server that supports GET and POST
 * requests.
 * It allows registering handlers for specific paths and starts listening for
 * incoming client connections.
 */
public class Api {
    private ServerSocket serverSocket;
    private Map<String, BiConsumer<Request, Response>> getHandlers = new HashMap<>();
    private Map<String, BiConsumer<Request, Response>> postHandlers = new HashMap<>();

    /**
     * Creates an instance of the Api class that listens on the specified port.
     *
     * @param port the port number on which the server will listen for incoming
     *             connections
     * @throws IOException if an I/O error occurs when opening the socket
     */
    public Api(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    /**
     * Registers a handler for GET requests on the specified path.
     *
     * @param path    the request path to handle
     * @param handler the handler to process the GET request
     */
    public void get(String path, BiConsumer<Request, Response> handler) {
        getHandlers.put(path, handler);
    }

    /**
     * Registers a handler for POST requests on the specified path.
     *
     * @param path    the request path to handle
     * @param handler the handler to process the POST request
     */
    public void post(String path, BiConsumer<Request, Response> handler) {
        postHandlers.put(path, handler);
    }

    /**
     * Starts the server and begins listening for incoming client connections.
     * The provided callback is run when the server starts listening.
     *
     * @param callback the callback to run when the server starts
     */
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
