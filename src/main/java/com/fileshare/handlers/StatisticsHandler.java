package com.fileshare.handlers;

import com.fileshare.core.Storage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class StatisticsHandler implements HttpHandler {
    private final Storage storage;

    public StatisticsHandler(Storage storage) { this.storage = storage; }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        List<Storage.Meta> metas = storage.listMetas();
        long totalFiles = metas.size();
        long totalBytes = metas.stream().mapToLong(m -> m.sizeBytes).sum();
        long totalDownloads = metas.stream().mapToLong(m -> m.downloadCount).sum();
        String json = "{\"totalFiles\":" + totalFiles + ",\"totalBytes\":" + totalBytes + ",\"totalDownloads\":" + totalDownloads + "}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}
