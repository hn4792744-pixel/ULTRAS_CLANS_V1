package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.AllianceBrowseGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class AllianceBrowseGui {

    public static final int SIZE = 54;
    public static final int SLOT_BACK = 49;

    private final UltrasClansPlugin plugin;

    public AllianceBrowseGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        List<Clan> candidates = new ArrayList<>();
        for (Clan other : plugin.getClanService().getAllClansSnapshot()) {
            if (other.getId().equals(clan.getId())) continue;
            if (plugin.getAllianceService().areAllied(clan.getId(), other.getId())) continue;
            candidates.add(other);
        }

        AllianceBrowseGuiHolder holder = new AllianceBrowseGuiHolder(player.getUniqueId(), clan.getId());
        holder.setCurrentEntries(candidates);
        Inventory inv = Bukkit.createInventory(holder, SIZE, "&8Propose Alliance");
        holder.setInventory(inv);

        int slot = 0;
        for (Clan other : candidates) {
            if (slot >= 45) break;
            OfflinePlayer owner = Bukkit.getOfflinePlayer(other.getOwnerUuid());
            inv.setItem(slot++, ItemBuilder.skullOf(owner)
                    .name("&f" + other.getColor() + other.getName())
                    .lore("&7Owner: &f" + (owner.getName() != null ? owner.getName() : "Unknown"),
                            "&7Members: &f" + plugin.getClanService().getMembers(other.getId()).size(),
                            "", "&eClick to propose an alliance!")
                    .build());
        }

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "general.back-button")).build());

        player.openInventory(inv);
    }
}
