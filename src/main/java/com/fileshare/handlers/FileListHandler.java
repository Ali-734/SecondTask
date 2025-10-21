package com.fileshare.handlers;

import com.fileshare.core.Storage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FileListHandler implements HttpHandler {
    private final Storage storage;

    public FileListHandler(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        
        try {
            List<Storage.Meta> metas = storage.listMetas();
            StringBuilder json = new StringBuilder();
            json.append("{\"files\":[");
            
            for (int i = 0; i < metas.size(); i++) {
                Storage.Meta meta = metas.get(i);
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"token\":\"").append(meta.token).append("\",");
                json.append("\"name\":\"").append(escapeJson(meta.originalName)).append("\",");
                json.append("\"size\":").append(meta.sizeBytes).append(",");
                json.append("\"downloads\":").append(meta.downloadCount).append(",");
                json.append("\"created\":").append(meta.createdAtEpochSec * 1000).append(",");
                json.append("\"lastDownloaded\":").append(meta.lastDownloadedEpochSec * 1000);
                json.append("}");
            }
            
            json.append("]}");
            
            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            String error = "{\"error\":\"Failed to get files list\"}";
            byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(500, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
}
