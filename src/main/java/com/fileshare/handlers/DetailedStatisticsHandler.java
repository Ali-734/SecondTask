package com.fileshare.handlers;

import com.fileshare.core.Storage;
import com.fileshare.core.Auth;
import com.fileshare.utils.HttpUtils;
import com.fileshare.utils.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Обработчик детальной статистики файлов
 */
public class DetailedStatisticsHandler implements HttpHandler {
    private final Storage storage;
    private final Auth auth;

    public DetailedStatisticsHandler(Storage storage, Auth auth) {
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
            String json = buildStatisticsJson(metas);
            sendJsonResponse(exchange, 200, json);
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, JsonUtils.createErrorJson("Failed to get file statistics"));
        }
    }
    
    private String buildStatisticsJson(List<Storage.Meta> metas) {
        if (metas.isEmpty()) {
            return buildEmptyStatisticsJson();
        }
        
        StatisticsData stats = calculateStatistics(metas);
        return buildJsonFromStatistics(stats, metas);
    }
    
    private String buildEmptyStatisticsJson() {
        return "{\"totalFiles\":0,\"totalSize\":0,\"totalDownloads\":0," +
               "\"sizeStats\":{\"max\":0,\"min\":0,\"median\":0,\"average\":0}," +
               "\"downloadStats\":{\"max\":0,\"min\":0,\"median\":0,\"average\":0}," +
               "\"timeStats\":{\"oldest\":0,\"newest\":0,\"medianAge\":0}," +
               "\"formatStats\":[]}";
    }
    
    private StatisticsData calculateStatistics(List<Storage.Meta> metas) {
        Map<String, Integer> formatCounts = new HashMap<>();
        Map<String, Long> formatSizes = new HashMap<>();
        
        long totalSize = 0, maxSize = 0, minSize = Long.MAX_VALUE;
        long totalDownloads = 0, maxDownloads = 0, minDownloads = Long.MAX_VALUE;
        long oldestFile = System.currentTimeMillis() / 1000, newestFile = 0;
        
        List<Long> sizes = new ArrayList<>();
        List<Long> downloads = new ArrayList<>();
        List<Long> ages = new ArrayList<>();
        
        for (Storage.Meta meta : metas) {
            String ext = getFileExtension(meta.originalName);
            formatCounts.put(ext, formatCounts.getOrDefault(ext, 0) + 1);
            formatSizes.put(ext, formatSizes.getOrDefault(ext, 0L) + meta.sizeBytes);
            
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
            ages.add(System.currentTimeMillis() / 1000 - meta.createdAtEpochSec);
        }
        
        Collections.sort(sizes);
        Collections.sort(downloads);
        Collections.sort(ages);
        
        return new StatisticsData(formatCounts, formatSizes, totalSize, maxSize, 
                                minSize == Long.MAX_VALUE ? 0 : minSize, sizes,
                                totalDownloads, maxDownloads, 
                                minDownloads == Long.MAX_VALUE ? 0 : minDownloads, downloads,
                                oldestFile, newestFile, ages);
    }
    
    private String buildJsonFromStatistics(StatisticsData stats, List<Storage.Meta> metas) {
        StringBuilder json = new StringBuilder();
        json.append("{\"totalFiles\":").append(metas.size()).append(",");
        json.append("\"totalSize\":").append(stats.totalSize).append(",");
        json.append("\"totalDownloads\":").append(stats.totalDownloads).append(",");
        
        // Size stats
        json.append("\"sizeStats\":{");
        json.append("\"max\":").append(stats.maxSize).append(",");
        json.append("\"min\":").append(stats.minSize).append(",");
        json.append("\"median\":").append(getMedian(stats.sizes)).append(",");
        json.append("\"average\":").append(metas.isEmpty() ? 0 : stats.totalSize / metas.size());
        json.append("},");
        
        // Download stats
        json.append("\"downloadStats\":{");
        json.append("\"max\":").append(stats.maxDownloads).append(",");
        json.append("\"min\":").append(stats.minDownloads).append(",");
        json.append("\"median\":").append(getMedian(stats.downloads)).append(",");
        json.append("\"average\":").append(metas.isEmpty() ? 0 : stats.totalDownloads / metas.size());
        json.append("},");
        
        // Time stats
        json.append("\"timeStats\":{");
        json.append("\"oldest\":").append(stats.oldestFile * 1000).append(",");
        json.append("\"newest\":").append(stats.newestFile * 1000).append(",");
        json.append("\"medianAge\":").append(getMedian(stats.ages));
        json.append("},");
        
        // Format stats
        json.append("\"formatStats\":[");
        List<Map.Entry<String, Integer>> sortedFormats = stats.formatCounts.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .collect(Collectors.toList());
        
        for (int i = 0; i < sortedFormats.size(); i++) {
            if (i > 0) json.append(",");
            Map.Entry<String, Integer> entry = sortedFormats.get(i);
            json.append("{\"format\":\"").append(entry.getKey()).append("\",");
            json.append("\"count\":").append(entry.getValue()).append(",");
            json.append("\"size\":").append(stats.formatSizes.get(entry.getKey()));
            json.append("}");
        }
        json.append("]}");
        
        return json.toString();
    }
    
    private long getMedian(List<Long> list) {
        return list.isEmpty() ? 0 : list.get(list.size() / 2);
    }
    
    private String getFileExtension(String filename) {
        return JsonUtils.getFileExtension(filename);
    }
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        HttpUtils.sendJsonResponse(exchange, statusCode, json);
    }
    
    private static class StatisticsData {
        final Map<String, Integer> formatCounts;
        final Map<String, Long> formatSizes;
        final long totalSize, maxSize, minSize;
        final List<Long> sizes;
        final long totalDownloads, maxDownloads, minDownloads;
        final List<Long> downloads;
        final long oldestFile, newestFile;
        final List<Long> ages;
        
        StatisticsData(Map<String, Integer> formatCounts, Map<String, Long> formatSizes,
                      long totalSize, long maxSize, long minSize, List<Long> sizes,
                      long totalDownloads, long maxDownloads, long minDownloads, List<Long> downloads,
                      long oldestFile, long newestFile, List<Long> ages) {
            this.formatCounts = formatCounts;
            this.formatSizes = formatSizes;
            this.totalSize = totalSize;
            this.maxSize = maxSize;
            this.minSize = minSize;
            this.sizes = sizes;
            this.totalDownloads = totalDownloads;
            this.maxDownloads = maxDownloads;
            this.minDownloads = minDownloads;
            this.downloads = downloads;
            this.oldestFile = oldestFile;
            this.newestFile = newestFile;
            this.ages = ages;
        }
    }
}
