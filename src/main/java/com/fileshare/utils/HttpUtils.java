package com.fileshare.utils;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Утилиты для работы с HTTP запросами
 */
public class HttpUtils {
    
    /**
     * Отправка JSON ответа
     */
    public static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    /**
     * Отправка JSON ответа с CORS заголовками
     */
    public static void sendJsonResponseWithCors(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    /**
     * Проверка HTTP метода
     */
    public static boolean isMethod(HttpExchange exchange, String method) {
        return method.equalsIgnoreCase(exchange.getRequestMethod());
    }
    
    /**
     * Отправка ошибки 405 (Method Not Allowed)
     */
    public static void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(405, -1);
    }
    
    /**
     * Отправка ошибки 400 (Bad Request)
     */
    public static void sendBadRequest(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(400, -1);
    }
    
    /**
     * Отправка ошибки 404 (Not Found)
     */
    public static void sendNotFound(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(404, -1);
    }
    
    /**
     * Отправка ошибки 401 (Unauthorized)
     */
    public static void sendUnauthorized(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(401, -1);
    }
    
    /**
     * Отправка ошибки 500 (Internal Server Error)
     */
    public static void sendInternalError(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(500, -1);
    }
}
