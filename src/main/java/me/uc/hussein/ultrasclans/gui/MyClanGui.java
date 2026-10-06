package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.MyClanGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * الواجهة الرئيسية للاعب الذي يملك كلانًا بالفعل (قسم 13).
 * في المرحلة الأولى: تُفعَّل Info/Members/Requests/Settings الأساسية،
 * بينما الأزرار الأخرى (Bank, Storage, Warps...) تظهر كـ"قريبًا" حتى
 * تُنفَّذ في مراحلها المخصصة.
 */
public final class MyClanGui {

    public static final int SIZE = 54;

    private final UltrasClansPlugin plugin;

    public MyClanGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        MyClanGuiHolder holder = new MyClanGuiHolder(player.getUniqueId(), clan.getId());
        String title = plugin.getMessageService().text(player, "general.gui-title-my-clan",
                java.util.Map.of("clan", clan.getName()));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        int members = plugin.getClanService().getMembers(clan.getId()).size();

        var tier = plugin.getRankTemplate().getTierForCp(clan.getCp());
        String tierDisplay = tier != null ? tier.getDisplay() : "&7Unranked";

        inv.setItem(10, new ItemBuilder(Material.PLAYER_HEAD)
                .name("&e&lClan Info")
                .lore("&7Name: &f" + clan.getName(),
                        "&7Members: &f" + members + "/" + clan.getMemberCapacity(),
                        "&7CP: &f" + clan.getCp() + " &8| " + tierDisplay,
                        "&7Level: &f" + clan.getClanLevel(),
                        "&7Public: &f" + (clan.isPublic() ? "Yes" : "No"))
                .build());

        inv.setItem(12, new ItemBuilder(Material.BOOK)
                .name("&b&lMembers")
                .lore("&7View and manage clan members.")
                .build());

        inv.setItem(14, new ItemBuilder(Material.WRITABLE_BOOK)
                .name("&a&lJoin Requests")
                .lore("&7Review pending join requests.")
                .build());

        inv.setItem(16, new ItemBuilder(Material.NAME_TAG)
                .name("&6&lInvite Member")
                .lore("&7Invite an online or offline player.")
                .build());

        inv.setItem(28, new ItemBuilder(Material.CHEST)
                .name("&e&lBank")
                .lore("&7Balance: &f" + plugin.getEconomyHook().format(clan.getBankBalance()),
                        "&7Capacity: &f" + plugin.getEconomyHook().format(clan.getBankCapacity()),
                        "", "&eClick to open!")
                .build());

        inv.setItem(30, new ItemBuilder(Material.BARREL)
                .name("&6&lStorage")
                .lore("&7Level: &f" + clan.getStorageLevel(), "", "&eClick to open!")
                .build());

        inv.setItem(32, new ItemBuilder(Material.NETHER_STAR)
                .name("&d&lUpgrades")
                .lore("&7CP: &f" + clan.getCp(), "", "&eClick to open!")
                .build());

        inv.setItem(22, new ItemBuilder(Material.DIAMOND_SWORD)
                .name("&c&lEvents")
                .lore("&7Challenge other clans to battle.", "", "&eClick to open!")
                .build());

        inv.setItem(31, new ItemBuilder(Material.WHITE_BANNER)
                .name("&b&lAlliances")
                .lore("&7Slots: &f" + plugin.getAllianceService().getAlliances(clan.getId()).size()
                        + "/" + clan.getAllianceCapacity(), "", "&eClick to open!")
                .build());

        inv.setItem(33, new ItemBuilder(Material.WRITABLE_BOOK)
                .name("&a&lTasks")
                .lore("&7Daily, weekly and monthly goals.", "", "&eClick to open!")
                .build());

        inv.setItem(34, new ItemBuilder(Material.COMPARATOR)
                .name("&c&lSettings")
                .lore("&7Use &f/clan settings &7to change:", "&7public/private, color, rename.")
                .build());

        inv.setItem(19, new ItemBuilder(Material.ENDER_PEARL)
                .name("&d&lWarps")
                .lore("&7Slots: &f" + clan.getWarpCapacity(), "", "&eClick to open!")
                .build());

        inv.setItem(21, new ItemBuilder(Material.ENDER_EYE)
                .name("&b&lTPA")
                .lore("&7Teleport to online clan members.", "", "&eClick to open!")
                .build());

        boolean clanChatOn = plugin.getChatService().isClanChatEnabled(player.getUniqueId());
        inv.setItem(23, new ItemBuilder(clanChatOn ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&a&lClan Chat: " + (clanChatOn ? "&aON" : "&7OFF"))
                .lore("&7Click to toggle.").build());

        boolean tagOn = plugin.getChatService().isTagEnabled(player.getUniqueId());
        inv.setItem(25, new ItemBuilder(tagOn ? Material.NAME_TAG : Material.GRAY_DYE)
                .name("&e&lClan Tag: " + (tagOn ? "&aON" : "&7OFF"))
                .lore("&7Click to toggle.").build());

        inv.setItem(49, new ItemBuilder(Material.BARRIER)
                .name("&c&lLeave Clan")
                .lore("&7Leave this clan.")
                .build());

        inv.setItem(18, new ItemBuilder(Material.PAPER).name("&f&lStatistics")
                .lore("&7Members, combat, bank and more.", "", "&eClick to open!").build());
        inv.setItem(20, new ItemBuilder(Material.GOLD_BLOCK).name("&6&lClan Top")
                .lore("&7Rankings of all clans.", "", "&eClick to open!").build());

        inv.setItem(53, new ItemBuilder(Material.COMPARATOR)
                .name(plugin.getMessageService().text(player, "settings.title"))
                .lore("&7/clansettings").build());

        player.openInventory(inv);
    }
}
