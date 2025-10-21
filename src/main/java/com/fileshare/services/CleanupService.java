package com.fileshare.services;

import com.fileshare.core.Storage;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

public final class CleanupService {
    private static final long PERIOD_MS = Duration.ofHours(12).toMillis();

    public static void start(Storage storage, Duration ttl) {
        Timer timer = new Timer("cleanup", true);
        timer.schedule(new TimerTask() {
            @Override public void run() {
                try {
                    int n = storage.deleteStale(ttl);
                    if (n > 0) System.out.println("Cleanup removed " + n + " stale files");
                } catch (Exception e) {
                    System.err.println("Cleanup error: " + e.getMessage());
                }
            }
        }, 10_000, PERIOD_MS);
    }
}
