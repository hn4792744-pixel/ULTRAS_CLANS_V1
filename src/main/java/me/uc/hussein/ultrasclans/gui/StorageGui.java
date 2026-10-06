package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.StorageGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public final class StorageGui {

    public static final int SIZE = 54;
    /** آخر صف (45-53) محجوز دائمًا للتحكم (زر رجوع)، وليس تخزينًا فعليًا. */
    public static final int MAX_USABLE_SLOTS = 45;
    public static final int SLOT_BACK = 49;

    private final UltrasClansPlugin plugin;

    public StorageGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(),
                me.uc.hussein.ultrasclans.model.ClanPermission.CLAN_STORAGE_VIEW)
                || plugin.getPermissionService().isOwner(player.getUniqueId());
        if (!allowed) {
            plugin.getMessageService().send(player, "storage.no-permission-view");
            return;
        }
        if (!plugin.getStorageSessions().tryAcquire(clan.getId(), player.getUniqueId())) {
            plugin.getMessageService().send(player, "storage.in-use");
            plugin.playSound(player, "error");
            return;
        }

        int unlocked = Math.min(MAX_USABLE_SLOTS,
                plugin.getStorageService().getUnlockedSlotCount(clan.getStorageLevel()));

        plugin.getStorageService().loadContents(clan.getId()).whenComplete((contents, error) ->
                plugin.getScheduler().runSync(() -> {
                    if (error != null || !player.isOnline()) {
                        // لا نفتح مخزنًا بلقطة فارغة عند فشل التحميل (قد يؤدي لمحو محتواه عند الحفظ)
                        plugin.getStorageSessions().release(clan.getId(), player.getUniqueId());
                        if (error != null) {
                            plugin.getMessageService().send(player, "errors.database-unavailable");
                        }
                        return;
                    }
                    render(player, clan, unlocked, contents);
                }));
    }

    private void render(Player player, Clan clan, int unlocked, Map<Integer, org.bukkit.inventory.ItemStack> contents) {
        StorageGuiHolder holder = new StorageGuiHolder(player.getUniqueId(), clan.getId(), unlocked);
        String title = plugin.getMessageService().text(player, "general.gui-title-storage",
                Map.of("clan", clan.getName(), "level", String.valueOf(clan.getStorageLevel())));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        var lockedItem = new ItemBuilder(Material.BARRIER)
                .name("&c&lLocked")
                .lore("&7Upgrade your storage level", "&7to unlock this slot.")
                .build();

        for (int slot = 0; slot < MAX_USABLE_SLOTS; slot++) {
            if (slot < unlocked) {
                var stored = contents.get(slot);
                if (stored != null) {
                    inv.setItem(slot, stored);
                }
                // else: يبقى فارغًا (interactive) - اللاعب يستطيع وضع أشياء فيه مباشرة
            } else {
                inv.setItem(slot, lockedItem);
            }
        }

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "general.back-button")).build());

        player.openInventory(inv);
    }
}
