package com.fileshare.utils;

public final class Environment {
    private Environment() {}

    public static String get(String key, String defaultValue) {
        String v = System.getenv(key);
        return v != null && !v.isBlank() ? v : defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String v = System.getenv(key);
        if (v == null) return defaultValue;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    public static long getLong(String key, long defaultValue) {
        String v = System.getenv(key);
        if (v == null) return defaultValue;
        try { return Long.parseLong(v.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }
}
