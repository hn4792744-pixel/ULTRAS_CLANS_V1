package me.uc.hussein.ultrasclans.security;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * يمنع تنفيذ إجراءات GUI متتالية بسرعة كبيرة من نفس اللاعب
 * (حماية أساسية من click-spam / double-click exploits - قسم 55/56).
 */
public final class ClickThrottle {

    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();
    private final long throttleMs;

    public ClickThrottle(long throttleMs) {
        this.throttleMs = throttleMs;
    }

    /**
     * @return true إذا كانت هذه النقرة مسموحة (ليست سريعة جدًا). يُسجّل الوقت تلقائيًا عند السماح.
     */
    public boolean attempt(UUID uuid) {
        long now = System.currentTimeMillis();
        Long last = lastClick.get(uuid);
        if (last != null && (now - last) < throttleMs) {
            return false;
        }
        lastClick.put(uuid, now);
        return true;
    }
}
