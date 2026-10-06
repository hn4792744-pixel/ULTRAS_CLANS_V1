package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.AlliancesGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanAlliance;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AlliancesGui {

    public static final int SIZE = 27;
    public static final int[] ALLIANCE_SLOTS = {10, 11, 12, 13, 14};
    public static final int SLOT_PROPOSE = 22;
    public static final int SLOT_BACK = 26;

    private final UltrasClansPlugin plugin;

    public AlliancesGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        List<ClanAlliance> alliances = plugin.getAllianceService().getAlliances(clan.getId());

        AlliancesGuiHolder holder = new AlliancesGuiHolder(player.getUniqueId(), clan.getId());
        holder.setCurrentEntries(alliances);
        String title = plugin.getMessageService().text(player, "general.gui-title-alliances", Map.of("clan", clan.getName()));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        for (int i = 0; i < ALLIANCE_SLOTS.length; i++) {
            if (i < alliances.size()) {
                ClanAlliance alliance = alliances.get(i);
                Optional<Clan> otherOpt = plugin.getClanService().getClan(alliance.otherClan(clan.getId()));
                if (otherOpt.isEmpty()) continue;
                Clan other = otherOpt.get();
                OfflinePlayer owner = Bukkit.getOfflinePlayer(other.getOwnerUuid());
                inv.setItem(ALLIANCE_SLOTS[i], ItemBuilder.skullOf(owner)
                        .name("&b&l" + other.getName())
                        .lore("&7Owner: &f" + (owner.getName() != null ? owner.getName() : "Unknown"),
                                "", "&eClick to manage!")
                        .build());
            } else {
                inv.setItem(ALLIANCE_SLOTS[i], new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                        .name("&7Empty Slot").build());
            }
        }

        int absoluteMax = plugin.getConfigManager().loadYaml("alliances.yml").getInt("max-alliances", 5);
        int effectiveMax = Math.min(absoluteMax, clan.getAllianceCapacity());
        inv.setItem(SLOT_PROPOSE, new ItemBuilder(Material.WHITE_BANNER)
                .name("&a&lPropose Alliance")
                .lore("&7Slots: &f" + alliances.size() + "/" + effectiveMax, "", "&eClick to browse clans!")
                .build());

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "general.back-button")).build());

        player.openInventory(inv);
    }
}
