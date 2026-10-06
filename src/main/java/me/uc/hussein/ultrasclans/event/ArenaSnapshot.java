package me.uc.hussein.ultrasclans.event;

import org.bukkit.block.data.BlockData;

/**
 * لقطة كتل منطقة الساحة الإدارية قبل بدء الحدث، لاستعادتها بعد انتهائه
 * (قسم 34). تُحفظ في الذاكرة فقط طوال مدة الحدث - راجع README لتوثيق
 * هذا القرار (لا تنجو من إعادة تشغيل السيرفر أثناء حدث نشط).
 */
public final class ArenaSnapshot {

    public record Entry(int x, int y, int z, BlockData data) {
    }

    private final String world;
    private final Entry[] entries;

    public ArenaSnapshot(String world, Entry[] entries) {
        this.world = world;
        this.entries = entries;
    }

    public String getWorld() {
        return world;
    }

    public Entry[] getEntries() {
        return entries;
    }
}
