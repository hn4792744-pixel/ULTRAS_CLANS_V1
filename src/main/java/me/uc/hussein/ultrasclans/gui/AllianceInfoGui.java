package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.AllianceInfoGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanAlliance;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public final class AllianceInfoGui {

    public static final int SIZE = 27;
    public static final int SLOT_MANAGE = 11;
    public static final int SLOT_DISBAND = 15;
    public static final int SLOT_BACK = 22;

    private final UltrasClansPlugin plugin;

    public AllianceInfoGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan, ClanAlliance alliance, Clan other) {
        AllianceInfoGuiHolder holder = new AllianceInfoGuiHolder(player.getUniqueId(), clan.getId(),
                alliance.getId(), other.getId());
        String title = plugin.getMessageService().text(player, "general.gui-title-alliance-info", Map.of("clan", other.getName()));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        OfflinePlayer owner = Bukkit.getOfflinePlayer(other.getOwnerUuid());
        int members = plugin.getClanService().getMembers(other.getId()).size();
        inv.setItem(4, ItemBuilder.skullOf(owner)
                .name("&b&l" + other.getName())
                .lore(
                        "&7Owner: &f" + (owner.getName() != null ? owner.getName() : "Unknown"),
                        "&7Members: &f" + members,
                        "&7CP: &f" + other.getCp()
                ).build());

        inv.setItem(SLOT_MANAGE, new ItemBuilder(Material.COMPARATOR)
                .name("&e&lManage My Grants")
                .lore("&7Choose what this clan may", "&7access from your clan.", "", "&eClick to open!")
                .build());

        inv.setItem(SLOT_DISBAND, new ItemBuilder(Material.BARRIER)
                .name("&c&lDisband Alliance")
                .lore("&7Ends the alliance for both clans.").build());

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "general.back-button")).build());

        player.openInventory(inv);
    }
}
