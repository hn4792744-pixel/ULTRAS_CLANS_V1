package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.ClanInfoGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class ClanInfoGui {

    public static final int SIZE = 27;
    public static final int SLOT_JOIN = 11;
    public static final int SLOT_MEMBERS = 13;
    public static final int SLOT_STATS = 15;
    public static final int SLOT_BACK = 22;

    private final UltrasClansPlugin plugin;

    public ClanInfoGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, Clan clan, boolean fromBrowser) {
        ClanInfoGuiHolder holder = new ClanInfoGuiHolder(viewer.getUniqueId(), clan.getId(), fromBrowser);
        String title = plugin.getMessageService().text(viewer, "general.gui-title-clan-info",
                java.util.Map.of("clan", clan.getName()));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        OfflinePlayer owner = Bukkit.getOfflinePlayer(clan.getOwnerUuid());
        int members = plugin.getClanService().getMembers(clan.getId()).size();
        String createdDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date(clan.getCreatedAt()));

        inv.setItem(4, ItemBuilder.skullOf(owner)
                .name("&f" + clan.getColor() + "&l" + clan.getName())
                .lore(
                        "&7Owner: &f" + (owner.getName() != null ? owner.getName() : "Unknown"),
                        "&7Members: &f" + members + "/" + clan.getMemberCapacity(),
                        "&7CP: &f" + clan.getCp(),
                        "&7Level: &f" + clan.getClanLevel(),
                        "&7Type: &f" + (clan.isPublic() ? "Public" : "Private"),
                        "&7Created: &f" + createdDate
                ).build());

        boolean viewerHasClan = plugin.getClanService().isInClan(viewer.getUniqueId());
        if (fromBrowser && !viewerHasClan) {
            if (clan.isPublic()) {
                inv.setItem(SLOT_JOIN, new ItemBuilder(Material.LIME_DYE)
                        .name("&a&lJoin Clan")
                        .lore("&7Click to join this clan directly.")
                        .build());
            } else {
                inv.setItem(SLOT_JOIN, new ItemBuilder(Material.YELLOW_DYE)
                        .name("&e&lRequest to Join")
                        .lore("&7This clan is private.", "&7Click to send a join request.")
                        .build());
            }
        }

        inv.setItem(SLOT_MEMBERS, new ItemBuilder(Material.BOOK)
                .name("&b&lMembers")
                .lore("&7View the member list.")
                .build());

        inv.setItem(SLOT_STATS, new ItemBuilder(Material.PAPER)
                .name("&e&lStatistics")
                .lore("&7Wins/Losses tracking arrives", "&7with the Events system.")
                .build());

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.BARRIER)
                .name(plugin.getMessageService().text(viewer, "general.back-button"))
                .build());

        viewer.openInventory(inv);
    }
}
