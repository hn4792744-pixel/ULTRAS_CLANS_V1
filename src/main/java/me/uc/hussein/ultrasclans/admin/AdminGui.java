package me.uc.hussein.ultrasclans.admin;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.AdminClanGuiHolder;
import me.uc.hussein.ultrasclans.gui.holder.AdminGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * واجهة الإدارة (قسم 44): قائمة كل الكلانات ثم لوحة تحكم لكل كلان. نصوصها
 * بالإنجليزية فقط (واجهة للمشرفين لا للاعبين). تتحقق دائمًا من صلاحية
 * ultras.clans.admin عند الفتح وعند كل نقرة (GuiClickListener).
 */
public final class AdminGui {

    public static final int LIST_SIZE = 54;
    public static final int SLOT_PREV = 45;
    public static final int SLOT_NEXT = 53;
    public static final int PAGE_SIZE = 45;

    public static final int CLAN_SIZE = 27;
    public static final int SLOT_CP_ADD = 10;
    public static final int SLOT_CP_REMOVE = 11;
    public static final int SLOT_BANK_ADD = 12;
    public static final int SLOT_BANK_REMOVE = 13;
    public static final int SLOT_INSPECT = 14;
    public static final int SLOT_RESET = 15;
    public static final int SLOT_DELETE = 16;
    public static final int SLOT_BACK = 22;

    public static final long CP_STEP = 100;
    public static final double BANK_STEP = 10_000;

    private final UltrasClansPlugin plugin;

    public AdminGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        if (!player.hasPermission("ultras.clans.admin")) {
            return;
        }
        List<Clan> all = new ArrayList<>(plugin.getClanService().getAllClansSnapshot());
        all.sort(Comparator.comparingLong(Clan::getCp).reversed());
        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) PAGE_SIZE));
        int safe = Math.max(0, Math.min(page, totalPages - 1));
        List<Clan> slice = all.subList(safe * PAGE_SIZE, Math.min(all.size(), (safe + 1) * PAGE_SIZE));

        AdminGuiHolder holder = new AdminGuiHolder(player.getUniqueId(), safe);
        holder.setCurrentEntries(new ArrayList<>(slice));
        Inventory inv = Bukkit.createInventory(holder, LIST_SIZE, "&8Admin - All Clans");
        holder.setInventory(inv);

        int slot = 0;
        for (Clan clan : slice) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(clan.getOwnerUuid());
            inv.setItem(slot++, ItemBuilder.skullOf(owner)
                    .name("&f" + clan.getName())
                    .lore("&7Owner: &f" + owner.getName(),
                            "&7Members: &f" + plugin.getClanService().getMembers(clan.getId()).size(),
                            "&7CP: &f" + clan.getCp(), "", "&eClick to manage!").build());
        }
        if (safe > 0) inv.setItem(SLOT_PREV, new ItemBuilder(Material.ARROW).name("&7« Previous Page").build());
        if (safe < totalPages - 1) inv.setItem(SLOT_NEXT, new ItemBuilder(Material.ARROW).name("&7Next Page »").build());
        player.openInventory(inv);
    }

    public void openClan(Player player, Clan clan) {
        if (!player.hasPermission("ultras.clans.admin")) {
            return;
        }
        AdminClanGuiHolder holder = new AdminClanGuiHolder(player.getUniqueId(), clan.getId());
        Inventory inv = Bukkit.createInventory(holder, CLAN_SIZE, "&8Admin - " + clan.getName());
        holder.setInventory(inv);

        inv.setItem(4, new ItemBuilder(Material.NETHER_STAR).name("&f" + clan.getName())
                .lore("&7CP: &f" + clan.getCp(), "&7Bank: &f" + clan.getBankBalance() + "/" + clan.getBankCapacity(),
                        "&7Level: &f" + clan.getClanLevel()).build());
        inv.setItem(SLOT_CP_ADD, new ItemBuilder(Material.LIME_DYE).name("&a+" + CP_STEP + " CP").build());
        inv.setItem(SLOT_CP_REMOVE, new ItemBuilder(Material.RED_DYE).name("&c-" + CP_STEP + " CP").build());
        inv.setItem(SLOT_BANK_ADD, new ItemBuilder(Material.GOLD_INGOT).name("&a+" + (long) BANK_STEP + " Bank").build());
        inv.setItem(SLOT_BANK_REMOVE, new ItemBuilder(Material.IRON_INGOT).name("&c-" + (long) BANK_STEP + " Bank").build());
        inv.setItem(SLOT_INSPECT, new ItemBuilder(Material.BOOK).name("&b&lInspect").lore("&7Prints details in chat.").build());
        inv.setItem(SLOT_RESET, new ItemBuilder(Material.ORANGE_WOOL).name("&6&lReset Clan").lore("&7Asks for confirmation.").build());
        inv.setItem(SLOT_DELETE, new ItemBuilder(Material.BARRIER).name("&4&lDelete Clan").lore("&7Asks for confirmation.").build());
        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW).name("&7« Back").build());
        player.openInventory(inv);
    }
}
