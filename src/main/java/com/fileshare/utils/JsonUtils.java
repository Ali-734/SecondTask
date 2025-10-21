package com.fileshare.utils;

/**
 * Утилиты для работы с JSON
 */
public class JsonUtils {
    
    /**
     * Экранирование строки для JSON
     */
    public static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
    /**
     * Получение расширения файла
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) return "unknown";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) return "no_extension";
        return filename.substring(lastDot + 1).toLowerCase();
    }
    
    /**
     * Создание JSON объекта с ошибкой
     */
    public static String createErrorJson(String message) {
        return "{\"error\":\"" + escapeJson(message) + "\"}";
    }
    
    /**
     * Создание JSON объекта с успехом
     */
    public static String createSuccessJson(String message) {
        return "{\"success\":true,\"message\":\"" + escapeJson(message) + "\"}";
    }
}
