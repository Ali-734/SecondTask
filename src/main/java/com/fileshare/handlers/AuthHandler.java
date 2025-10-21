package com.fileshare.handlers;

import com.fileshare.core.TokenManager;
import com.fileshare.utils.HttpUtils;
import com.fileshare.utils.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Обработчик авторизации
 * Принимает имя пользователя и возвращает токен доступа
 */
public class AuthHandler implements HttpHandler {
    private final TokenManager tokenManager;
    
    public AuthHandler(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!HttpUtils.isMethod(exchange, "POST")) {
            HttpUtils.sendMethodNotAllowed(exchange);
            return;
        }
        
        try {
            // Читаем тело запроса
            String body;
            try (InputStream requestBody = exchange.getRequestBody();
                 Scanner scanner = new Scanner(requestBody, StandardCharsets.UTF_8)) {
                body = scanner.useDelimiter("\\A").next();
            }
            
            // Парсим JSON (простой парсинг для username)
            String username = parseUsernameFromJson(body);
            
            if (username == null || username.trim().isEmpty()) {
                HttpUtils.sendJsonResponseWithCors(exchange, 400, JsonUtils.createErrorJson("Username is required"));
                return;
            }
            
            // Генерируем токен
            String token = tokenManager.generateToken(username.trim());
            
            // Отправляем ответ с токеном
            String response = "{\"token\":\"" + token + "\",\"username\":\"" + username.trim() + "\"}";
            HttpUtils.sendJsonResponseWithCors(exchange, 200, response);
            
        } catch (Exception e) {
            HttpUtils.sendJsonResponseWithCors(exchange, 500, JsonUtils.createErrorJson("Internal server error"));
        }
    }
    
    private String parseUsernameFromJson(String json) {
        try {
            // Простой парсинг JSON для извлечения username
            int usernameIndex = json.indexOf("\"username\":");
            if (usernameIndex == -1) return null;
            
            int startQuote = json.indexOf("\"", usernameIndex + 11);
            if (startQuote == -1) return null;
            
            int endQuote = json.indexOf("\"", startQuote + 1);
            if (endQuote == -1) return null;
            
            return json.substring(startQuote + 1, endQuote);
        } catch (Exception e) {
            return null;
        }
    }
    
}
