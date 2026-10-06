package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.StatsGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanMember;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/** إحصائيات حقيقية للكلان من البيانات المتاحة فقط (Wins/Losses/Mob kills غير متتبَّعة بعد). */
public final class StatsGui {

    public static final int SIZE = 27;
    public static final int SLOT_BACK = 22;

    private final UltrasClansPlugin plugin;

    public StatsGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        StatsGuiHolder holder = new StatsGuiHolder(player.getUniqueId(), clan.getId());
        Inventory inv = Bukkit.createInventory(holder, SIZE, "&8" + clan.getName() + " - Statistics");
        holder.setInventory(inv);

        List<ClanMember> members = plugin.getClanService().getMembers(clan.getId());
        long online = members.stream().filter(m -> plugin.getServer().getPlayer(m.getUuid()) != null).count();
        int kills = members.stream().mapToInt(ClanMember::getKills).sum();
        int deaths = members.stream().mapToInt(ClanMember::getDeaths).sum();
        String kd = deaths == 0 ? String.valueOf(kills) : String.format("%.2f", kills / (double) deaths);
        var tier = plugin.getRankTemplate().getTierForCp(clan.getCp());

        inv.setItem(10, new ItemBuilder(Material.PLAYER_HEAD).name("&b&lMembers")
                .lore("&7Total: &f" + members.size() + "/" + clan.getMemberCapacity(), "&7Online: &a" + online).build());
        inv.setItem(11, new ItemBuilder(Material.NETHER_STAR).name("&d&lProgress")
                .lore("&7CP: &f" + clan.getCp(), "&7Tier: &f" + (tier != null ? tier.getName() : "-"),
                        "&7Level: &f" + clan.getClanLevel()).build());
        inv.setItem(12, new ItemBuilder(Material.DIAMOND_SWORD).name("&c&lCombat")
                .lore("&7Kills: &f" + kills, "&7Deaths: &f" + deaths, "&7K/D: &f" + kd).build());
        inv.setItem(13, new ItemBuilder(Material.GOLD_INGOT).name("&e&lBank")
                .lore("&7Balance: &f" + plugin.getEconomyHook().format(clan.getBankBalance()),
                        "&7Capacity: &f" + plugin.getEconomyHook().format(clan.getBankCapacity())).build());
        inv.setItem(14, new ItemBuilder(Material.BARREL).name("&6&lCapacity")
                .lore("&7Storage level: &f" + clan.getStorageLevel(), "&7Warp slots: &f" + clan.getWarpCapacity(),
                        "&7Alliance slots: &f" + clan.getAllianceCapacity(),
                        "&7Alliances: &f" + plugin.getAllianceService().getAlliances(clan.getId()).size()).build());

        members.stream().max(Comparator.comparingInt(ClanMember::getKills)).ifPresent(top ->
                inv.setItem(15, new ItemBuilder(Material.GOLDEN_SWORD).name("&6&lTop Fighter")
                        .lore("&f" + Bukkit.getOfflinePlayer(top.getUuid()).getName(), "&7Kills: &f" + top.getKills()).build()));
        inv.setItem(16, new ItemBuilder(Material.CLOCK).name("&7&lCreated")
                .lore("&f" + new SimpleDateFormat("yyyy-MM-dd").format(new Date(clan.getCreatedAt()))).build());

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "settings.back")).build());
        player.openInventory(inv);
    }
}
