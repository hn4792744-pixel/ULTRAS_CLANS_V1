package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.WarpsGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanWarp;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

public final class WarpsGui {

    public static final int SIZE = 27;
    public static final int SLOT_CREATE = 22;
    public static final int SLOT_BACK = 26;
    public static final int[] WARP_SLOTS = {10, 11, 12, 13, 14};

    private final UltrasClansPlugin plugin;

    public WarpsGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        plugin.getWarpService().listWarps(clan.getId()).whenComplete((warps, error) ->
                plugin.getScheduler().runSync(() -> render(player, clan, error == null ? warps : List.of())));
    }

    private void render(Player player, Clan clan, List<ClanWarp> warps) {
        WarpsGuiHolder holder = new WarpsGuiHolder(player.getUniqueId(), clan.getId());
        holder.setCurrentEntries(warps);
        String title = plugin.getMessageService().text(player, "general.gui-title-warps", Map.of("clan", clan.getName()));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        for (int i = 0; i < WARP_SLOTS.length; i++) {
            if (i < warps.size()) {
                ClanWarp warp = warps.get(i);
                inv.setItem(WARP_SLOTS[i], new ItemBuilder(Material.ENDER_PEARL)
                        .name("&d&lWarp #" + warp.getWarpId())
                        .lore(
                                "&7World: &f" + warp.getWorld(),
                                "",
                                "&eLeft-click to teleport",
                                "&cShift + Left-click to delete"
                        ).build());
            } else {
                inv.setItem(WARP_SLOTS[i], new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                        .name("&7Empty Slot").build());
            }
        }

        inv.setItem(SLOT_CREATE, new ItemBuilder(Material.EMERALD)
                .name("&a&lCreate Warp Here")
                .lore("&7Uses: &f" + warps.size() + "/" + clan.getWarpCapacity(),
                        "", "&eClick to create!")
                .build());

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "general.back-button")).build());

        player.openInventory(inv);
    }
}
