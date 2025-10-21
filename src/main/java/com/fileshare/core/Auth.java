package com.fileshare.core;

import com.sun.net.httpserver.HttpExchange;

public class Auth {
    private final String uploadToken;

    public Auth(String uploadToken) {
        this.uploadToken = uploadToken;
    }

    public boolean isEnabled() {
        return uploadToken != null && !uploadToken.isBlank();
    }

    public boolean checkUpload(HttpExchange exchange) {
        if (!isEnabled()) return true;
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null) return false;
        String expected = "Bearer " + uploadToken;
        return header.equals(expected);
    }
}
