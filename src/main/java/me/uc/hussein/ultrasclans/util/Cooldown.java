package me.uc.hussein.ultrasclans.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * أداة عامة لإدارة فترات الانتظار (cooldowns) لأي إجراء داخل البلاجن.
 * Thread-safe: يمكن استدعاؤها من أي Thread.
 */
public final class Cooldown {

    private final Map<UUID, Long> expiryTimestamps = new ConcurrentHashMap<>();

    /**
     * @return true إذا كان اللاعب لا يزال ضمن فترة الانتظار
     */
    public boolean isOnCooldown(UUID uuid) {
        Long expiry = expiryTimestamps.get(uuid);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() >= expiry) {
            expiryTimestamps.remove(uuid);
            return false;
        }
        return true;
    }

    public long remainingSeconds(UUID uuid) {
        Long expiry = expiryTimestamps.get(uuid);
        if (expiry == null) {
            return 0;
        }
        long remainingMs = expiry - System.currentTimeMillis();
        return Math.max(0, remainingMs / 1000);
    }

    public void set(UUID uuid, long durationSeconds) {
        expiryTimestamps.put(uuid, System.currentTimeMillis() + (durationSeconds * 1000));
    }

    public void clear(UUID uuid) {
        expiryTimestamps.remove(uuid);
    }
}
