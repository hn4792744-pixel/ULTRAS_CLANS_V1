package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.ChallengeBrowseGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * اختيار الكلان الذي سيُتحدّى. يستثني كلانك نفسه والكلانات المتحالفة معه
 * (قسم 32: "لا يمكن Challenge Alliance") والكلانات الموجودة في حدث بالفعل.
 */
public final class ChallengeBrowseGui {

    public static final int SIZE = 54;
    public static final int SLOT_BACK = 49;

    private final UltrasClansPlugin plugin;

    public ChallengeBrowseGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        List<Clan> candidates = new ArrayList<>();
        for (Clan other : plugin.getClanService().getAllClansSnapshot()) {
            if (other.getId().equals(clan.getId())) continue;
            if (plugin.getAllianceService().areAllied(clan.getId(), other.getId())) continue;
            if (!plugin.getEventService().getActiveOrPendingForClan(other.getId()).isEmpty()) continue;
            candidates.add(other);
        }

        ChallengeBrowseGuiHolder holder = new ChallengeBrowseGuiHolder(player.getUniqueId(), clan.getId());
        holder.setCurrentEntries(candidates);
        Inventory inv = Bukkit.createInventory(holder, SIZE, "&8Challenge a Clan");
        holder.setInventory(inv);

        int slot = 0;
        for (Clan other : candidates) {
            if (slot >= 45) break;
            OfflinePlayer owner = Bukkit.getOfflinePlayer(other.getOwnerUuid());
            long online = plugin.getClanService().getMembers(other.getId()).stream()
                    .filter(m -> plugin.getServer().getPlayer(m.getUuid()) != null).count();
            inv.setItem(slot++, ItemBuilder.skullOf(owner)
                    .name("&f" + other.getColor() + other.getName())
                    .lore("&7Online members: &a" + online, "&7CP: &f" + other.getCp(),
                            "", "&eClick to set up a challenge!").build());
        }

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "general.back-button")).build());
        player.openInventory(inv);
    }
}
