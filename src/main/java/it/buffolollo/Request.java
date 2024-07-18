package it.buffolollo;

import java.util.HashMap;
import java.util.Map;

public class Request {
    private String method;
    private String path;
    public Map<String, String> params = new HashMap<>();
    public Map<String, String> body = new HashMap<>();
    public Map<String, String> headers = new HashMap<>();

    public Request(String method, String path) {
        this.method = method;
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
}