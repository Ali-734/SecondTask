package com.fileshare.handlers;

import com.fileshare.core.Storage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public class FileDownloadHandler implements HttpHandler {
    private final Storage storage;

    public FileDownloadHandler(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        String[] parts = path.split("/");
        if (parts.length < 3) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        String token = parts[2];
        Storage.Meta meta = storage.readMeta(token);
        if (meta == null) {
            byte[] notFound = "Not found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, notFound.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(notFound); }
            return;
        }
        Path filePath = storage.filePath(token);
        if (!Files.exists(filePath)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        storage.touchDownload(token);
        exchange.getResponseHeaders().add("Content-Type", meta.contentType);
        String filename = meta.originalName == null || meta.originalName.isBlank() ? token : meta.originalName;
        String encodedFilename = encodeFilename(filename);
        exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"");
        exchange.sendResponseHeaders(200, Files.size(filePath));
        try (OutputStream os = exchange.getResponseBody()) {
            Files.copy(filePath, os);
        }
    }
    
    private static String encodeFilename(String filename) {
        if (filename == null) return "";
        
        boolean isAscii = filename.chars().allMatch(c -> c < 128);
        
        if (isAscii) {
            return filename.replace("\"", "\\\"");
        } else {
            try {
                return "=?UTF-8?B?" + java.util.Base64.getEncoder().encodeToString(filename.getBytes(StandardCharsets.UTF_8)) + "?=";
            } catch (Exception e) {
                return filename.replace("\"", "\\\"");
            }
        }
    }
}
