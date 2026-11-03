package it.buffolollo;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class Response {
    private Socket clientSocket;
    private PrintWriter out;
    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    int status = 200;

    public Response(Socket clientSocket, PrintWriter out) {
        this.clientSocket = clientSocket;
        this.out = out;
    }

    public Response status(int status) {
        this.status = status;
        return this;
    }

    public Response header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public void send(String body) {
        out.println("HTTP/1.1 " + status + " OK");
        out.println("Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length);
        if (!headers.containsKey("Content-Type")) {
            String contentType;
            try {
                JsonParser.parseString(body);

                contentType = "application/json";
            } catch (JsonSyntaxException e) {
                if (body.contains("<html>")) {
                    contentType = "text/html";
                } else {
                    contentType = "text/plain";
                }
            }
            out.println("Content-Type: " + contentType);
        }
        for (Map.Entry<String, String> header : headers.entrySet()) {
            out.println(header.getKey() + ": " + header.getValue());
        }

        out.println();

        out.println(body);

        close();
    }

    private void close() {
        try {
            out.close();
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
