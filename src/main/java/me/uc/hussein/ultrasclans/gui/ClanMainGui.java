package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.ClanMainGuiHolder;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * الواجهة الرئيسية للاعب الذي لا يملك كلانًا (قسم 5).
 */
public final class ClanMainGui {

    public static final int SIZE = 27;
    public static final int SLOT_CREATE = 11;
    public static final int SLOT_JOIN = 13;
    public static final int SLOT_INVITES = 15;
    public static final int SLOT_TOP = 20;
    public static final int SLOT_HELP = 24;

    private final UltrasClansPlugin plugin;

    public ClanMainGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        ClanMainGuiHolder holder = new ClanMainGuiHolder(player.getUniqueId());
        String title = plugin.getMessageService().text(player, "general.gui-title-clan-main");
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        inv.setItem(SLOT_CREATE, new ItemBuilder(Material.NETHER_STAR)
                .name("&a&lCreate Clan")
                .lore("&7Start your own clan.", "", "&eClick to create!")
                .build());

        inv.setItem(SLOT_JOIN, new ItemBuilder(Material.COMPASS)
                .name("&b&lBrowse Clans")
                .lore("&7View and join an existing clan.", "", "&eClick to browse!")
                .build());

        inv.setItem(SLOT_INVITES, new ItemBuilder(Material.PAPER)
                .name("&e&lMy Invites")
                .lore("&7View pending clan invitations.", "", "&eClick to view!")
                .build());

        inv.setItem(SLOT_TOP, new ItemBuilder(Material.GOLD_INGOT)
                .name("&6&lClan Top")
                .lore("&7See the best clans on the server.", "", "&eClick to view!")
                .build());

        inv.setItem(SLOT_HELP, new ItemBuilder(Material.BOOK)
                .name("&7&lHelp")
                .lore("&7View all clan commands.")
                .build());

        inv.setItem(22, new ItemBuilder(Material.COMPARATOR)
                .name(plugin.getMessageService().text(player, "settings.title"))
                .lore("&7/clansettings").build());

        player.openInventory(inv);
    }
}
