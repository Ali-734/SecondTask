package com.fileshare.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Storage {
    public static class Meta {
        public final String token;
        public final String originalName;
        public final String contentType;
        public final long sizeBytes;
        public final long createdAtEpochSec;
        public final long lastDownloadedEpochSec;
        public final long downloadCount;

        public Meta(String token, String originalName, String contentType, long sizeBytes,
                    long createdAtEpochSec, long lastDownloadedEpochSec, long downloadCount) {
            this.token = token;
            this.originalName = originalName;
            this.contentType = contentType;
            this.sizeBytes = sizeBytes;
            this.createdAtEpochSec = createdAtEpochSec;
            this.lastDownloadedEpochSec = lastDownloadedEpochSec;
            this.downloadCount = downloadCount;
        }
    }

    private final Path filesDir;
    private final Path metaDir;

    public Storage(Path filesDir, Path metaDir) {
        this.filesDir = Objects.requireNonNull(filesDir);
        this.metaDir = Objects.requireNonNull(metaDir);
    }

    public String saveUploadedFile(InputStream data, String originalName, String contentType) throws IOException {
        String token = UUID.randomUUID().toString().replace("-", "");
        Path filePath = filePath(token);
        try (BufferedInputStream bis = new BufferedInputStream(data);
             OutputStream os = new BufferedOutputStream(Files.newOutputStream(filePath))) {
            long size = bis.transferTo(os);
            os.flush();
            long now = Instant.now().getEpochSecond();
            Meta m = new Meta(token, originalName, contentType == null ? "application/octet-stream" : contentType,
                    size, now, 0, 0);
            writeMeta(m);
            return token;
        }
    }

    public Path filePath(String token) { return filesDir.resolve(token + ".bin"); }
    public Path metaPath(String token) { return metaDir.resolve(token + ".meta"); }

    public Meta readMeta(String token) throws IOException {
        Path p = metaPath(token);
        if (!Files.exists(p)) return null;
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        String originalName = null, contentType = null;
        long size = 0, created = 0, last = 0, count = 0;
        for (String line : lines) {
            int i = line.indexOf('=');
            if (i <= 0) continue;
            String k = line.substring(0, i);
            String v = line.substring(i + 1);
            switch (k) {
                case "token" -> {
                    // ignored, trust parameter
                }
                case "originalName" -> originalName = v;
                case "contentType" -> contentType = v;
                case "sizeBytes" -> size = parseLong(v);
                case "createdAtEpochSec" -> created = parseLong(v);
                case "lastDownloadedEpochSec" -> last = parseLong(v);
                case "downloadCount" -> count = parseLong(v);
            }
        }
        return new Meta(token, originalName, contentType, size, created, last, count);
    }

    public void touchDownload(String token) throws IOException {
        Meta m = readMeta(token);
        if (m == null) return;
        Meta updated = new Meta(m.token, m.originalName, m.contentType, m.sizeBytes,
                m.createdAtEpochSec, Instant.now().getEpochSecond(), m.downloadCount + 1);
        writeMeta(updated);
    }

    public void writeMeta(Meta m) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("token=").append(m.token).append('\n');
        sb.append("originalName=").append(m.originalName == null ? "" : m.originalName).append('\n');
        sb.append("contentType=").append(m.contentType == null ? "" : m.contentType).append('\n');
        sb.append("sizeBytes=").append(m.sizeBytes).append('\n');
        sb.append("createdAtEpochSec=").append(m.createdAtEpochSec).append('\n');
        sb.append("lastDownloadedEpochSec=").append(m.lastDownloadedEpochSec).append('\n');
        sb.append("downloadCount=").append(m.downloadCount).append('\n');
        Files.writeString(metaPath(m.token), sb.toString(), StandardCharsets.UTF_8);
    }

    public List<Meta> listMetas() throws IOException {
        List<Meta> list = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(metaDir, "*.meta")) {
            for (Path p : ds) {
                String name = p.getFileName().toString();
                String token = name.substring(0, name.length() - 5);
                Meta m = readMeta(token);
                if (m != null) list.add(m);
            }
        }
        return list;
    }

    public int deleteStale(Duration ttl) throws IOException {
        long now = Instant.now().getEpochSecond();
        int deleted = 0;
        for (Meta m : listMetas()) {
            if (now - m.lastDownloadedEpochSec >= ttl.toSeconds()) {
                Files.deleteIfExists(filePath(m.token));
                Files.deleteIfExists(metaPath(m.token));
                deleted++;
            }
        }
        return deleted;
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0L; }
    }
}
