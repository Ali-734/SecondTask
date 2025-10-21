package com.fileshare.handlers;

import com.fileshare.core.Storage;
import com.fileshare.core.Auth;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class FileUploadHandler implements HttpHandler {
    private final Storage storage;
    private final Auth auth;

    public FileUploadHandler(Storage storage, Auth auth) {
        this.storage = storage;
        this.auth = auth;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        if (!auth.checkUpload(exchange)) {
            exchange.sendResponseHeaders(401, -1);
            return;
        }
        String ct = exchange.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
            sendJson(exchange, 400, "{\"error\":\"multipart/form-data required\"}");
            return;
        }
        String boundary = parseBoundary(ct);
        if (boundary == null) {
            sendJson(exchange, 400, "{\"error\":\"boundary not found\"}");
            return;
        }

        Part filePart = parseMultipart(exchange.getRequestBody(), boundary);
        if (filePart == null) {
            sendJson(exchange, 400, "{\"error\":\"file part not found\"}");
            return;
        }

        String token = storage.saveUploadedFile(filePart.dataStream(), filePart.filename, filePart.contentType);
        String host = exchange.getRequestHeaders().getFirst("X-Forwarded-Host");
        if (host == null) {
            String hostHeader = exchange.getRequestHeaders().getFirst("Host");
            if (hostHeader != null && !hostHeader.isEmpty()) {
                host = hostHeader;
            } else {
                host = "localhost:" + exchange.getLocalAddress().getPort();
            }
        }
        String scheme = exchange.getRequestHeaders().getFirst("X-Forwarded-Proto");
        if (scheme == null) scheme = "http";
        String downloadUrl = scheme + "://" + host + "/d/" + token;
        String json = "{\"token\":\"" + token + "\",\"url\":\"" + downloadUrl + "\"}";
        Headers headers = exchange.getResponseHeaders();
        headers.add("Content-Type", "application/json; charset=utf-8");
        sendJson(exchange, 200, json);
    }

    private static String parseBoundary(String contentType) {
        for (String part : contentType.split(";")) {
            String t = part.trim();
            if (t.startsWith("boundary=")) {
                String b = t.substring("boundary=".length());
                if (b.startsWith("\"") && b.endsWith("\"")) b = b.substring(1, b.length()-1);
                return b;
            }
        }
        return null;
    }

    private record Part(String filename, String contentType, byte[] data) {
        InputStream dataStream() { return new java.io.ByteArrayInputStream(data); }
    }

    private static Part parseMultipart(InputStream in, String boundary) throws IOException {
        byte[] body = in.readAllBytes();
        byte[] delimiterBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        byte[] crlf = "\r\n".getBytes(StandardCharsets.UTF_8);
        byte[] crlfcrlf = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);
        
        int start = findBytes(body, delimiterBytes, 0);
        if (start < 0) return null;
        start += delimiterBytes.length;
        
        while (start < body.length) {
            if (startsWith(body, start, crlf)) {
                start += crlf.length;
            }
            
            int headerEnd = findBytes(body, crlfcrlf, start);
            if (headerEnd < 0) break;
            
            String headers = new String(body, start, headerEnd - start, StandardCharsets.UTF_8);
            
            int dataStart = headerEnd + crlfcrlf.length;

            byte[] boundaryPrefix = new String("\r\n").getBytes(StandardCharsets.UTF_8);
            byte[] boundaryMarker = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
            int searchPos = dataStart;
            int boundaryStart = -1;
            while (true) {
                int crlfPos = findBytes(body, boundaryPrefix, searchPos);
                if (crlfPos < 0) break;
                int possibleBoundary = crlfPos + boundaryPrefix.length;
                if (startsWith(body, possibleBoundary, boundaryMarker)) {
                    boundaryStart = crlfPos;
                    break;
                }
                searchPos = crlfPos + 1;
            }
            if (boundaryStart < 0) {
                break;
            }

            int dataEnd = boundaryStart;

            int afterBoundary = boundaryStart + boundaryPrefix.length + boundaryMarker.length;
            boolean isLast = startsWith(body, afterBoundary, new byte[]{'-','-'});
            if (isLast) {
            } else {
                int nextStart = afterBoundary;
                if (startsWith(body, nextStart, new byte[]{'\r','\n'})) {
                    nextStart += 2;
                }
                start = nextStart;
            }
            
            String cd = null;
            String ctype = null;
            for (String h : headers.split("\r\n")) {
                String[] kv = h.split(":", 2);
                if (kv.length != 2) continue;
                String k = kv[0].trim().toLowerCase(Locale.ROOT);
                String v = kv[1].trim();
                if (k.equals("content-disposition")) cd = v;
                if (k.equals("content-type")) ctype = v;
            }
            
            String filename = null;
            if (cd != null) {
                for (String attr : cd.split(";")) {
                    String t = attr.trim();
                    if (t.startsWith("filename=")) {
                        filename = t.substring("filename=".length());
                        if (filename.startsWith("\"") && filename.endsWith("\"")) {
                            filename = filename.substring(1, filename.length()-1);
                        }
                        try {
                            filename = java.net.URLDecoder.decode(filename, StandardCharsets.UTF_8);
                        } catch (Exception e) {
                        }
                    }
                }
            }
            
            if (filename != null && !filename.isEmpty()) {
                byte[] data = new byte[dataEnd - dataStart];
                System.arraycopy(body, dataStart, data, 0, data.length);
                return new Part(filename, ctype, data);
            }

            if (isLast) {
                break;
            }
        }
        return null;
    }
    
    private static int findBytes(byte[] haystack, byte[] needle, int start) {
        for (int i = start; i <= haystack.length - needle.length; i++) {
            boolean found = true;
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }
    
    private static boolean startsWith(byte[] array, int offset, byte[] prefix) {
        if (offset + prefix.length > array.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (array[offset + i] != prefix[i]) return false;
        }
        return true;
    }

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
