package com.fileshare.handlers;

import com.fileshare.core.Storage;
import com.fileshare.core.Auth;
import com.fileshare.utils.HttpUtils;
import com.fileshare.utils.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;

/**
 * Обработчик списка файлов
 */
public class FileListHandler implements HttpHandler {
    private final Storage storage;
    private final Auth auth;

    public FileListHandler(Storage storage, Auth auth) {
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
        
        try {
            List<Storage.Meta> metas = storage.listMetas();
            String json = buildFilesListJson(metas);
            HttpUtils.sendJsonResponse(exchange, 200, json);
        } catch (Exception e) {
            HttpUtils.sendJsonResponse(exchange, 500, JsonUtils.createErrorJson("Failed to get files list"));
        }
    }
    
    private String buildFilesListJson(List<Storage.Meta> metas) {
        StringBuilder json = new StringBuilder();
        json.append("{\"files\":[");
        
        for (int i = 0; i < metas.size(); i++) {
            Storage.Meta meta = metas.get(i);
            if (i > 0) json.append(",");
            
            json.append("{")
                .append("\"token\":\"").append(meta.token).append("\",")
                .append("\"name\":\"").append(JsonUtils.escapeJson(meta.originalName)).append("\",")
                .append("\"size\":").append(meta.sizeBytes).append(",")
                .append("\"downloads\":").append(meta.downloadCount).append(",")
                .append("\"created\":").append(meta.createdAtEpochSec * 1000).append(",")
                .append("\"lastDownloaded\":").append(meta.lastDownloadedEpochSec * 1000)
                .append("}");
        }
        
        json.append("]}");
        return json.toString();
    }
}
