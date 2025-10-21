package com.fileshare.handlers;

import com.fileshare.core.Storage;
import com.fileshare.utils.HttpUtils;
import com.fileshare.utils.JsonUtils;
import com.fileshare.utils.PathUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Обработчик удаления файлов
 */
public class FileDeleteHandler implements HttpHandler {
    private final Storage storage;

    public FileDeleteHandler(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!HttpUtils.isMethod(exchange, "DELETE")) {
            HttpUtils.sendMethodNotAllowed(exchange);
            return;
        }
        
        String token = PathUtils.extractTokenFromDeletePath(exchange.getRequestURI().getPath());
        if (token == null) {
            HttpUtils.sendBadRequest(exchange);
            return;
        }
        
        try {
            if (storage.readMeta(token) == null) {
                HttpUtils.sendJsonResponse(exchange, 404, JsonUtils.createErrorJson("File not found"));
                return;
            }
            
            deleteFileAndMeta(token);
            HttpUtils.sendJsonResponse(exchange, 200, "{\"success\":true}");
        } catch (Exception e) {
            HttpUtils.sendJsonResponse(exchange, 500, JsonUtils.createErrorJson("Failed to delete file"));
        }
    }
    
    private void deleteFileAndMeta(String token) throws IOException {
        Files.deleteIfExists(storage.filePath(token));
        Files.deleteIfExists(storage.metaPath(token));
    }
}
