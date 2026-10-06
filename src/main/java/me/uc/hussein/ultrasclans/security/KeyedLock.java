package me.uc.hussein.ultrasclans.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * قفل عام مبني على مفتاح (مثال: clanId, "clanId:invite:playerUuid").
 * يُستخدم لمنع تنفيذ عمليتين متزامنتين على نفس الكيان (race conditions)،
 * مثل إرسال نفس الدعوة مرتين في نفس اللحظة أو سحب/إيداع بنك متزامن.
 */
public final class KeyedLock<K> {

    private final Map<K, Boolean> locks = new ConcurrentHashMap<>();

    public boolean tryLock(K key) {
        return locks.putIfAbsent(key, Boolean.TRUE) == null;
    }

    public void unlock(K key) {
        locks.remove(key);
    }
}
