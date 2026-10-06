package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.TopGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanMember;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** ترتيب الكلانات حسب فئات حقيقية محسوبة من البيانات الحالية (قسم 28 - الفئات المدعومة فقط). */
public final class TopGui {

    public enum Category {
        CP("CP", Material.NETHER_STAR), LEVEL("Clan Level", Material.EXPERIENCE_BOTTLE),
        MEMBERS("Members", Material.PLAYER_HEAD), BANK("Bank", Material.GOLD_INGOT),
        KILLS("Kills", Material.DIAMOND_SWORD), DEATHS("Deaths", Material.SKELETON_SKULL),
        STORAGE("Storage Level", Material.BARREL);

        final String label;
        final Material icon;

        Category(String label, Material icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    public static final int SIZE = 54;
    public static final int[] CATEGORY_SLOTS = {1, 2, 3, 4, 5, 6, 7};
    public static final int SLOT_BACK = 49;

    private final UltrasClansPlugin plugin;

    public TopGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Category category) {
        TopGuiHolder holder = new TopGuiHolder(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, SIZE, "&8Clan Top - " + category.label);
        holder.setInventory(inv);

        Category[] all = Category.values();
        for (int i = 0; i < all.length; i++) {
            inv.setItem(CATEGORY_SLOTS[i], new ItemBuilder(all[i].icon)
                    .name((all[i] == category ? "&a&l" : "&f") + all[i].label)
                    .lore(all[i] == category ? "&7Showing now" : "&eClick to view").glow(all[i] == category).build());
        }

        List<Clan> sorted = new ArrayList<>(plugin.getClanService().getAllClansSnapshot());
        sorted.sort(Comparator.comparingDouble((Clan c) -> value(c, category)).reversed());

        Optional<Clan> own = plugin.getClanService().getPlayerClan(player.getUniqueId());
        int slot = 9;
        for (int rank = 1; rank <= sorted.size() && slot < 45; rank++, slot++) {
            Clan clan = sorted.get(rank - 1);
            boolean mine = own.isPresent() && own.get().getId().equals(clan.getId());
            inv.setItem(slot, new ItemBuilder(rank <= 3 ? Material.GOLD_BLOCK : Material.IRON_BLOCK)
                    .name((mine ? "&a" : "&f") + "#" + rank + " " + clan.getName())
                    .lore("&7" + category.label + ": &f" + format(value(clan, category), category),
                            "&7Owner: &f" + Bukkit.getOfflinePlayer(clan.getOwnerUuid()).getName())
                    .amount(Math.min(rank, 64)).glow(mine).build());
        }
        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "settings.back")).build());
        player.openInventory(inv);
    }

    public Category categoryAt(int slot) {
        Category[] all = Category.values();
        for (int i = 0; i < all.length; i++) {
            if (CATEGORY_SLOTS[i] == slot) return all[i];
        }
        return null;
    }

    private double value(Clan clan, Category category) {
        return switch (category) {
            case CP -> clan.getCp();
            case LEVEL -> clan.getClanLevel();
            case MEMBERS -> plugin.getClanService().getMembers(clan.getId()).size();
            case BANK -> clan.getBankBalance();
            case KILLS -> plugin.getClanService().getMembers(clan.getId()).stream().mapToInt(ClanMember::getKills).sum();
            case DEATHS -> plugin.getClanService().getMembers(clan.getId()).stream().mapToInt(ClanMember::getDeaths).sum();
            case STORAGE -> clan.getStorageLevel();
        };
    }

    private String format(double value, Category category) {
        return category == Category.BANK ? plugin.getEconomyHook().format(value) : String.valueOf((long) value);
    }
}
