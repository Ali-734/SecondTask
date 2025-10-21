package com.fileshare.handlers;

import com.fileshare.core.Storage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class DetailedStatisticsHandler implements HttpHandler {
    private final Storage storage;

    public DetailedStatisticsHandler(Storage storage) {
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
            
            Map<String, Integer> formatStats = new HashMap<>();
            Map<String, Long> formatSizeStats = new HashMap<>();
            
            long totalSize = 0;
            long maxSize = 0;
            long minSize = Long.MAX_VALUE;
            List<Long> sizes = new ArrayList<>();
            
            long totalDownloads = 0;
            long maxDownloads = 0;
            long minDownloads = Long.MAX_VALUE;
            List<Long> downloads = new ArrayList<>();
            
            long now = System.currentTimeMillis() / 1000;
            long oldestFile = now;
            long newestFile = 0;
            List<Long> ages = new ArrayList<>();
            
            for (Storage.Meta meta : metas) {
                String extension = getFileExtension(meta.originalName);
                formatStats.put(extension, formatStats.getOrDefault(extension, 0) + 1);
                formatSizeStats.put(extension, formatSizeStats.getOrDefault(extension, 0L) + meta.sizeBytes);
                
                totalSize += meta.sizeBytes;
                maxSize = Math.max(maxSize, meta.sizeBytes);
                minSize = Math.min(minSize, meta.sizeBytes);
                sizes.add(meta.sizeBytes);
                
                totalDownloads += meta.downloadCount;
                maxDownloads = Math.max(maxDownloads, meta.downloadCount);
                minDownloads = Math.min(minDownloads, meta.downloadCount);
                downloads.add(meta.downloadCount);
                
                oldestFile = Math.min(oldestFile, meta.createdAtEpochSec);
                newestFile = Math.max(newestFile, meta.createdAtEpochSec);
                ages.add(now - meta.createdAtEpochSec);
            }
            
            List<Map.Entry<String, Integer>> sortedFormats = formatStats.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
            
            Collections.sort(sizes);
            Collections.sort(downloads);
            Collections.sort(ages);
            
            long medianSize = sizes.isEmpty() ? 0 : sizes.get(sizes.size() / 2);
            long medianDownloads = downloads.isEmpty() ? 0 : downloads.get(downloads.size() / 2);
            long medianAge = ages.isEmpty() ? 0 : ages.get(ages.size() / 2);
            
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"totalFiles\":").append(metas.size()).append(",");
            json.append("\"totalSize\":").append(totalSize).append(",");
            json.append("\"totalDownloads\":").append(totalDownloads).append(",");
            json.append("\"sizeStats\":{");
            json.append("\"max\":").append(maxSize).append(",");
            json.append("\"min\":").append(minSize == Long.MAX_VALUE ? 0 : minSize).append(",");
            json.append("\"median\":").append(medianSize).append(",");
            json.append("\"average\":").append(metas.isEmpty() ? 0 : totalSize / metas.size());
            json.append("},");
            json.append("\"downloadStats\":{");
            json.append("\"max\":").append(maxDownloads).append(",");
            json.append("\"min\":").append(minDownloads == Long.MAX_VALUE ? 0 : minDownloads).append(",");
            json.append("\"median\":").append(medianDownloads).append(",");
            json.append("\"average\":").append(metas.isEmpty() ? 0 : totalDownloads / metas.size());
            json.append("},");
            json.append("\"timeStats\":{");
            if (metas.isEmpty()) {
                json.append("\"oldest\":0,");
                json.append("\"newest\":0,");
            } else {
                json.append("\"oldest\":").append(oldestFile * 1000).append(",");
                json.append("\"newest\":").append(newestFile * 1000).append(",");
            }
            json.append("\"medianAge\":").append(medianAge);
            json.append("},");
            json.append("\"formatStats\":[");
            for (int i = 0; i < sortedFormats.size(); i++) {
                if (i > 0) json.append(",");
                Map.Entry<String, Integer> entry = sortedFormats.get(i);
                json.append("{");
                json.append("\"format\":\"").append(entry.getKey()).append("\",");
                json.append("\"count\":").append(entry.getValue()).append(",");
                json.append("\"size\":").append(formatSizeStats.get(entry.getKey()));
                json.append("}");
            }
            json.append("]");
            json.append("}");
            
            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            String error = "{\"error\":\"Failed to get file statistics\"}";
            byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(500, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) return "unknown";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) return "no_extension";
        return filename.substring(lastDot + 1).toLowerCase();
    }
}
