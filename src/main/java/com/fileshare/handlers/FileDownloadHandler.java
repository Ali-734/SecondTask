package com.fileshare.handlers;

import com.fileshare.core.Storage;
import com.fileshare.utils.HttpUtils;
import com.fileshare.utils.PathUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/**
 * Обработчик скачивания файлов
 */
public class FileDownloadHandler implements HttpHandler {
    private final Storage storage;

    public FileDownloadHandler(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!HttpUtils.isMethod(exchange, "GET")) {
            HttpUtils.sendMethodNotAllowed(exchange);
            return;
        }
        
        String token = PathUtils.extractTokenFromDownloadPath(exchange.getRequestURI().getPath());
        if (token == null) {
            HttpUtils.sendBadRequest(exchange);
            return;
        }
        
        Storage.Meta meta = storage.readMeta(token);
        if (meta == null || !Files.exists(storage.filePath(token))) {
            sendNotFoundResponse(exchange);
            return;
        }
        
        serveFile(exchange, token, meta);
    }
    
    private void sendNotFoundResponse(HttpExchange exchange) throws IOException {
        byte[] notFound = "Not found".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(404, notFound.length);
        try (var os = exchange.getResponseBody()) {
            os.write(notFound);
        }
    }
    
    private void serveFile(HttpExchange exchange, String token, Storage.Meta meta) throws IOException {
        storage.touchDownload(token);
        
        Path filePath = storage.filePath(token);
        String filename = meta.originalName == null || meta.originalName.isBlank() ? token : meta.originalName;
        
        exchange.getResponseHeaders().add("Content-Type", meta.contentType);
        exchange.getResponseHeaders().add("Content-Disposition", 
            "attachment; filename=\"" + encodeFilename(filename) + "\"");
        
        exchange.sendResponseHeaders(200, Files.size(filePath));
        try (var os = exchange.getResponseBody()) {
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
                return "=?UTF-8?B?" + java.util.Base64.getEncoder()
                    .encodeToString(filename.getBytes(StandardCharsets.UTF_8)) + "?=";
            } catch (Exception e) {
                return filename.replace("\"", "\\\"");
            }
        }
    }
}
