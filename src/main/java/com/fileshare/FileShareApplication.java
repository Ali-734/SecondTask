package com.fileshare;

import com.fileshare.core.Storage;
import com.fileshare.core.Auth;
import com.fileshare.core.TokenManager;
import com.fileshare.handlers.*;
import com.fileshare.services.CleanupService;
import com.fileshare.utils.Environment;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileShareApplication {
    public static void main(String[] args) throws IOException {
        int port = Environment.getInt("PORT", 8080);
        Path dataDir = Path.of(Environment.get("DATA_DIR", "data"));
        Path filesDir = dataDir.resolve("files");
        Path metaDir = dataDir.resolve("meta");
        
        try {
            Files.createDirectories(filesDir);
            Files.createDirectories(metaDir);
        } catch (IOException e) {
            throw new IOException("Failed to create data directories: " + e.getMessage(), e);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        Storage storage = new Storage(filesDir, metaDir);
        
        // Настройка системы авторизации
        boolean authEnabled = Environment.get("AUTH_ENABLED", "true").equalsIgnoreCase("true");
        long tokenExpirationHours = Environment.getLong("TOKEN_EXPIRATION_HOURS", 24);
        TokenManager tokenManager = new TokenManager(tokenExpirationHours);
        Auth auth = new Auth(tokenManager, authEnabled);

        // Static files
        server.createContext("/", new StaticFileHandler());

        // API endpoints
        server.createContext("/api/auth", new AuthHandler(tokenManager));
        server.createContext("/api/upload", new FileUploadHandler(storage, auth));
        server.createContext("/api/stats", new StatisticsHandler(storage, auth));
        server.createContext("/api/files", new FileListHandler(storage, auth));
        server.createContext("/api/delete", new FileDeleteHandler(storage));
        server.createContext("/api/file-stats", new DetailedStatisticsHandler(storage, auth));

        // File downloads
        server.createContext("/d", new FileDownloadHandler(storage));

        // Cleanup scheduler
        long daysToLive = Environment.getLong("DAYS_TO_LIVE", 30);
        Duration ttl = Duration.ofDays(daysToLive);
        CleanupService.start(storage, ttl);
        
        // Token cleanup scheduler
        ScheduledExecutorService tokenCleanupExecutor = Executors.newScheduledThreadPool(1);
        tokenCleanupExecutor.scheduleAtFixedRate(
            tokenManager::cleanupExpiredTokens,
            1, 1, TimeUnit.HOURS
        );

        server.setExecutor(null);
        System.out.println("FileShare server started on port " + port);
        System.out.println("Data directory: " + dataDir.toAbsolutePath());
        String authInfo = auth.isEnabled() ? "enabled (token-based)" : "disabled";
        System.out.println("Upload auth: " + authInfo);
        if (auth.isEnabled()) {
            System.out.println("Token expiration: " + tokenExpirationHours + " hours");
        }
        server.start();
    }
}
