package me.uc.hussein.ultrasclans.listener;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.StorageGuiHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * عند إغلاق واجهة المخزن (بأي سبب: إغلاق يدوي، خروج، teleport، موت...)
 * تُحفظ الخانات المفتوحة نهائيًا ثم يُحرَّر قفل المشاهد الوحيد (قسم 55).
 */
public final class StorageCloseListener implements Listener {

    private final UltrasClansPlugin plugin;

    public StorageCloseListener(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof StorageGuiHolder holder)) {
            return;
        }
        plugin.getStorageSessions().saveNow(holder);
        plugin.getStorageSessions().release(holder.getClanId(), holder.getOwnerUuid());
    }
}
