package com.fileshare.core;

import com.sun.net.httpserver.HttpExchange;

/**
 * Система авторизации
 * Проверяет токены в заголовках запросов
 */
public class Auth {
    private final TokenManager tokenManager;
    private final boolean authEnabled;

    public Auth(TokenManager tokenManager, boolean authEnabled) {
        this.tokenManager = tokenManager;
        this.authEnabled = authEnabled;
    }

    public boolean isEnabled() {
        return authEnabled;
    }

    public boolean checkUpload(HttpExchange exchange) {
        if (!isEnabled()) return true;
        
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null) return false;
        
        if (!header.startsWith("Bearer ")) return false;
        
        String token = header.substring(7); // Убираем "Bearer "
        return tokenManager.validateToken(token);
    }
    
    public String getUsername(HttpExchange exchange) {
        if (!isEnabled()) return null;
        
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        
        String token = header.substring(7);
        return tokenManager.getUsername(token);
    }
}
