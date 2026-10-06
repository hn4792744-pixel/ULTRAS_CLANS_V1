package me.uc.hussein.ultrasclans.storage;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.StorageGuiHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * يمنع تكرار العناصر (duplication) في Storage عبر ثلاث حمايات (قسم 55):
 * 1) لاعب واحد فقط يفتح مخزن الكلان في نفس الوقت (لا لقطات قديمة متعارضة).
 * 2) حفظ مؤجل (debounce) بعد كل حركة فعلية حتى لا تضيع التغييرات عند
 *    crash قبل إغلاق الواجهة.
 * 3) حفظ نهائي عند الإغلاق.
 */
public final class StorageSessionManager {

    private static final long AUTOSAVE_DELAY_TICKS = 10L;

    private final UltrasClansPlugin plugin;
    private final Map<UUID, UUID> activeViewers = new ConcurrentHashMap<>();
    private final Set<UUID> pendingSave = ConcurrentHashMap.newKeySet();

    public StorageSessionManager(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @return true فقط إذا كان المخزن غير مستخدم إطلاقًا. حتى نفس اللاعب لا
     * يحصل عليه مرتين (يمنع فتح واجهتين متتاليتين تُحرّر إحداهما القفل وتترك
     * الأخرى مفتوحة بلا حماية).
     */
    public boolean tryAcquire(UUID clanId, UUID viewer) {
        return activeViewers.putIfAbsent(clanId, viewer) == null;
    }

    public UUID currentViewer(UUID clanId) {
        return activeViewers.get(clanId);
    }

    public void release(UUID clanId, UUID viewer) {
        activeViewers.remove(clanId, viewer);
    }

    /** يجدول حفظًا مؤجلًا (مرة واحدة كل فترة قصيرة مهما كثرت النقرات). */
    public void markDirty(StorageGuiHolder holder) {
        UUID clanId = holder.getClanId();
        if (!pendingSave.add(clanId)) {
            return; // حفظ مجدول بالفعل
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingSave.remove(clanId);
            saveNow(holder);
        }, AUTOSAVE_DELAY_TICKS);
    }

    /** يأخذ لقطة من الخانات المفتوحة (main thread فقط) ويحفظها بالتسلسل. */
    public void saveNow(StorageGuiHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) {
            return;
        }
        Map<Integer, ItemStack> snapshot = new HashMap<>();
        for (int slot = 0; slot < holder.getUnlockedSlots(); slot++) {
            ItemStack item = inv.getItem(slot);
            snapshot.put(slot, item == null ? null : item.clone());
        }
        plugin.getStorageService().saveContents(holder.getClanId(), snapshot);
    }
}
