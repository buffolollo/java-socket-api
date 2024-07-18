package it.buffolollo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private Map<String, BiConsumer<Request, Response>> getHandlers = new HashMap<>();
    private Map<String, BiConsumer<Request, Response>> postHandlers = new HashMap<>();

    public ClientHandler(Socket clientSocket, Map<String, BiConsumer<Request, Response>> getHandlers,
            Map<String, BiConsumer<Request, Response>> postHandlers) {
        this.clientSocket = clientSocket;
        this.getHandlers = getHandlers;
        this.postHandlers = postHandlers;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String requestLine = in.readLine();
            if (requestLine != null) {
                String method = requestLine.split(" ")[0];
                String path = requestLine.split(" ")[1];

                Request req = new Request(method, path);

                if ("GET".equals(method)) {
                    handleGetRequest(clientSocket, path, in, out, req);
                } else if ("POST".equals(method)) {
                    handlePostRequest(clientSocket, path, in, out);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleGetRequest(Socket clientSocket, String path, BufferedReader in, PrintWriter out, Request req) {
        Response res = new Response(clientSocket, out);

        try {
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                int delimiter = line.indexOf(":");

                if (delimiter == -1) {
                    continue;
                }

                String headerName = line.substring(0, delimiter).trim();
                String headerValue = line.substring(delimiter + 1).trim();

                req.headers.put(headerName, headerValue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] pathParts = path.split("/");

        for (String registeredPath : getHandlers.keySet()) {
            String[] registeredParts = registeredPath.split("/");
            if (pathParts.length == registeredParts.length) {
                boolean isMatch = true;
                for (int i = 0; i < registeredParts.length; i++) {
                    if (registeredParts[i].startsWith(":")) {
                        String paramName = registeredParts[i].substring(1);

                        req.params.put(paramName, pathParts[i]);
                    } else if (!registeredParts[i].equals(pathParts[i])) {
                        isMatch = false;
                        break;
                    }
                }
                if (isMatch) {
                    getHandlers.get(registeredPath).accept(req, res);

                    return;
                }
            }
        }

        if (getHandlers.containsKey(path)) {
            getHandlers.get(path).accept(req, res);
        } else {
            handleNotFound(res);
        }
    }

    private void handlePostRequest(Socket clientSocket, String path, BufferedReader in, PrintWriter out) {
        Response res = new Response(clientSocket, out);
        Request req = new Request("POST", path);

        try {
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                int delimiter = line.indexOf(":");

                if (delimiter == -1) {
                    continue;
                }

                String headerName = line.substring(0, delimiter).trim();
                String headerValue = line.substring(delimiter + 1).trim();

                req.headers.put(headerName, headerValue);
            }

            String contentType = req.headers.get("Content-Type");
            String contentLengthS = req.headers.get("Content-Length");

            int contentLength = -1;
            if (contentLengthS != null) {
                try {
                    contentLength = Integer.parseInt(contentLengthS);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            if (contentType != null && contentType.startsWith("multipart/form-data")) {
                System.out.println("Multipart form data");
                // String boundary = contentType.split("boundary=")[1];
                // handleMultipartFormData(in, boundary);
            } else if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
                handleUrlEncodedFormData(in, contentLength, req);
            }

            String[] pathParts = path.split("/");
            boolean handlerFound = false;

            for (String registeredPath : postHandlers.keySet()) {
                String[] registeredParts = registeredPath.split("/");
                if (pathParts.length == registeredParts.length) {
                    boolean isMatch = true;
                    for (int i = 0; i < registeredParts.length; i++) {
                        if (registeredParts[i].startsWith(":")) {
                            String paramName = registeredParts[i].substring(1);
                            req.params.put(paramName, pathParts[i]);
                        } else if (!registeredParts[i].equals(pathParts[i])) {
                            isMatch = false;
                            break;
                        }
                    }
                    if (isMatch) {
                        postHandlers.get(registeredPath).accept(req, res);
                        handlerFound = true;
                        break;
                    }
                }
            }

            if (!handlerFound) {
                handleNotFound(res);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handleUrlEncodedFormData(BufferedReader in, int contentLength, Request req) throws IOException {
        if (contentLength > 0) {
            char[] buffer = new char[contentLength];
            in.read(buffer);

            String data = new String(buffer);
            String[] pairs = data.split("&");

            for (String pair : pairs) {
                String[] keyValue = pair.split("=");

                req.body.put(keyValue[0], keyValue[1]);
            }
        }
    }

    private void handleNotFound(Response res) {
        res.status(404).send("<html><body><h1>Not found</h1></body></html>");
    }
}
