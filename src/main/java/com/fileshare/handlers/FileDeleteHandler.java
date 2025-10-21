package com.fileshare.handlers;

import com.fileshare.core.Storage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileDeleteHandler implements HttpHandler {
    private final Storage storage;

    public FileDeleteHandler(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length < 4) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        
        String token = parts[3];
        
        try {
            if (storage.readMeta(token) == null) {
                String error = "{\"error\":\"File not found\"}";
                byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }
            
            Files.deleteIfExists(storage.filePath(token));
            Files.deleteIfExists(storage.metaPath(token));
            
            String success = "{\"success\":true}";
            byte[] bytes = success.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            String error = "{\"error\":\"Failed to delete file\"}";
            byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(500, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
