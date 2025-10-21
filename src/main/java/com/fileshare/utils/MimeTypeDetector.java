package com.fileshare.utils;

public final class MimeTypeDetector {
    private MimeTypeDetector() {}

    public static String guess(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".html") || p.endsWith(".htm")) return "text/html; charset=utf-8";
        if (p.endsWith(".css")) return "text/css; charset=utf-8";
        if (p.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (p.endsWith(".json")) return "application/json; charset=utf-8";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".gif")) return "image/gif";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".txt")) return "text/plain; charset=utf-8";
        if (p.endsWith(".pdf")) return "application/pdf";
        if (p.endsWith(".doc")) return "application/msword";
        if (p.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (p.endsWith(".xls")) return "application/vnd.ms-excel";
        if (p.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (p.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (p.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (p.endsWith(".zip")) return "application/zip";
        if (p.endsWith(".rar")) return "application/x-rar-compressed";
        if (p.endsWith(".7z")) return "application/x-7z-compressed";
        return "application/octet-stream";
    }
}
