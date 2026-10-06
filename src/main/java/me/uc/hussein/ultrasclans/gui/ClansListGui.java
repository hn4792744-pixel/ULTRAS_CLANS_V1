package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.ClansListGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * واجهة /clans - تصفح جميع الكلانات مع Pagination وSearch (قسم 4).
 */
public final class ClansListGui {

    public static final int SIZE = 54;
    private static final int PAGE_SIZE = 45; // 5 صفوف للكلانات + صف تحكم سفلي
    public static final int SLOT_PREV = 45;
    public static final int SLOT_SEARCH = 49;
    public static final int SLOT_NEXT = 53;
    public static final int SLOT_BACK = 48;

    private final UltrasClansPlugin plugin;

    public ClansListGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page, String searchQuery) {
        List<Clan> all = plugin.getClanService().getAllClansSnapshot();
        List<Clan> filtered = new ArrayList<>();
        for (Clan clan : all) {
            if (searchQuery == null || searchQuery.isBlank()
                    || clan.getName().toLowerCase(Locale.ROOT).contains(searchQuery.toLowerCase(Locale.ROOT))) {
                filtered.add(clan);
            }
        }
        filtered.sort((a, b) -> Long.compare(b.getCp(), a.getCp()));

        int totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PAGE_SIZE;
        int to = Math.min(filtered.size(), from + PAGE_SIZE);
        List<Clan> pageEntries = filtered.subList(from, to);

        ClansListGuiHolder holder = new ClansListGuiHolder(player.getUniqueId(), safePage, searchQuery);
        holder.setCurrentPageEntries(new ArrayList<>(pageEntries));
        String title = plugin.getMessageService().text(player, "general.gui-title-clans-list");
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        int slot = 0;
        for (Clan clan : pageEntries) {
            inv.setItem(slot++, buildClanItem(clan));
        }

        if (safePage > 0) {
            inv.setItem(SLOT_PREV, new ItemBuilder(Material.ARROW).name("&7« Previous Page").build());
        }
        if (safePage < totalPages - 1) {
            inv.setItem(SLOT_NEXT, new ItemBuilder(Material.ARROW).name("&7Next Page »").build());
        }
        inv.setItem(SLOT_SEARCH, new ItemBuilder(Material.COMPASS)
                .name("&e&lSearch")
                .lore("&7Current: &f" + (searchQuery == null || searchQuery.isBlank() ? "None" : searchQuery),
                        "", "&eClick to search by name.")
                .build());
        inv.setItem(SLOT_BACK, new ItemBuilder(Material.BARRIER).name(plugin.getMessageService().text(player, "general.back-button")).build());

        player.openInventory(inv);
    }

    private ItemStack buildClanItem(Clan clan) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(clan.getOwnerUuid());
        int online = (int) plugin.getClanService().getMembers(clan.getId()).stream()
                .filter(m -> Bukkit.getPlayer(m.getUuid()) != null).count();
        int members = plugin.getClanService().getMembers(clan.getId()).size();

        return ItemBuilder.skullOf(owner)
                .name("&f" + clan.getColor() + clan.getName())
                .lore(
                        "&7Owner: &f" + (owner.getName() != null ? owner.getName() : "Unknown"),
                        "&7Members: &f" + members + "/" + clan.getMemberCapacity(),
                        "&7Online: &a" + online,
                        "&7CP: &f" + clan.getCp(),
                        "&7Type: &f" + (clan.isPublic() ? "Public" : "Private"),
                        "",
                        "&eClick to view!"
                ).build();
    }
}
