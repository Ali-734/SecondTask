package com.fileshare.core;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Менеджер токенов авторизации
 * Генерирует, валидирует и управляет токенами доступа
 */
public class TokenManager {
    private final ConcurrentMap<String, TokenInfo> tokens = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final long tokenExpirationHours;
    
    public TokenManager(long tokenExpirationHours) {
        this.tokenExpirationHours = tokenExpirationHours;
    }
    
    // Генерация нового токена для пользователя
    public String generateToken(String username) {
        // Генерируем случайный токен
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        // Сохраняем информацию о токене
        TokenInfo tokenInfo = new TokenInfo(username, Instant.now().plusSeconds(tokenExpirationHours * 3600));
        tokens.put(token, tokenInfo);
        
        return token;
    }
    
    // Проверка валидности токена
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        
        TokenInfo tokenInfo = tokens.get(token);
        if (tokenInfo == null) {
            return false;
        }
        
        // Проверяем срок действия токена
        if (Instant.now().isAfter(tokenInfo.expirationTime)) {
            tokens.remove(token); // Удаляем просроченный токен
            return false;
        }
        
        return true;
    }
    
    public String getUsername(String token) {
        TokenInfo tokenInfo = tokens.get(token);
        return tokenInfo != null ? tokenInfo.username : null;
    }
    
    public void revokeToken(String token) {
        tokens.remove(token);
    }
    
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        tokens.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expirationTime));
    }
    
    private static class TokenInfo {
        final String username;
        final Instant expirationTime;
        
        TokenInfo(String username, Instant expirationTime) {
            this.username = username;
            this.expirationTime = expirationTime;
        }
    }
}
