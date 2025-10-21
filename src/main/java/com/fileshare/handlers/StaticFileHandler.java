package com.fileshare.handlers;

import com.fileshare.utils.MimeTypeDetector;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class StaticFileHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        if (path.equals("/")) path = "/index.html";
        if (path.contains("..")) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        String resourcePath = "/public" + path;
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                byte[] notFound = "Not found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, notFound.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(notFound); }
                return;
            }
            String contentType = MimeTypeDetector.guess(path);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                in.transferTo(os);
            }
        }
    }
}
