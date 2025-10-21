package com.fileshare.handlers;

import com.fileshare.core.Storage;
import com.fileshare.core.Auth;
import com.fileshare.utils.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;

/**
 * Обработчик базовой статистики файлов
 */
public class StatisticsHandler implements HttpHandler {
    private final Storage storage;
    private final Auth auth;

    public StatisticsHandler(Storage storage, Auth auth) { 
        this.storage = storage; 
        this.auth = auth;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!HttpUtils.isMethod(exchange, "GET")) {
            HttpUtils.sendMethodNotAllowed(exchange);
            return;
        }
        
        // Проверяем авторизацию
        if (!auth.checkUpload(exchange)) {
            HttpUtils.sendUnauthorized(exchange);
            return;
        }
        
        List<Storage.Meta> metas = storage.listMetas();
        String json = buildStatisticsJson(metas);
        HttpUtils.sendJsonResponse(exchange, 200, json);
    }
    
    private String buildStatisticsJson(List<Storage.Meta> metas) {
        long totalFiles = metas.size();
        long totalBytes = metas.stream().mapToLong(m -> m.sizeBytes).sum();
        long totalDownloads = metas.stream().mapToLong(m -> m.downloadCount).sum();
        
        return String.format("{\"totalFiles\":%d,\"totalBytes\":%d,\"totalDownloads\":%d}", 
                           totalFiles, totalBytes, totalDownloads);
    }
}
