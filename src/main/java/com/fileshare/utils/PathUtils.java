package com.fileshare.utils;

/**
 * Утилиты для работы с путями
 */
public class PathUtils {
    
    /**
     * Извлечение токена из пути /d/{token}
     */
    public static String extractTokenFromDownloadPath(String path) {
        String[] parts = path.split("/");
        return parts.length >= 3 ? parts[2] : null;
    }
    
    /**
     * Извлечение токена из пути /api/delete/{token}
     */
    public static String extractTokenFromDeletePath(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : null;
    }
}
