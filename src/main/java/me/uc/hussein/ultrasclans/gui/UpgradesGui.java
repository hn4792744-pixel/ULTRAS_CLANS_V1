package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.UpgradesGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.upgrades.UpgradeType;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

public final class UpgradesGui {

    public static final int SIZE = 27;
    public static final Map<Integer, UpgradeType> SLOT_TYPES = Map.of(
            10, UpgradeType.MEMBER_CAPACITY,
            11, UpgradeType.BANK_CAPACITY,
            12, UpgradeType.STORAGE_LEVEL,
            14, UpgradeType.WARP_CAPACITY,
            15, UpgradeType.ALLIANCE_CAPACITY,
            16, UpgradeType.CLAN_LEVEL
    );
    public static final int SLOT_BACK = 22;

    private final UltrasClansPlugin plugin;

    public UpgradesGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        UpgradesGuiHolder holder = new UpgradesGuiHolder(player.getUniqueId(), clan.getId());
        String title = plugin.getMessageService().text(player, "general.gui-title-upgrades", Map.of("clan", clan.getName()));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        inv.setItem(4, new ItemBuilder(Material.NETHER_STAR)
                .name("&d&lClan Points")
                .lore("&7Available: &f" + clan.getCp() + " CP").build());

        for (var entry : SLOT_TYPES.entrySet()) {
            inv.setItem(entry.getKey(), buildUpgradeItem(clan, entry.getValue()));
        }

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "general.back-button")).build());

        player.openInventory(inv);
    }

    private org.bukkit.inventory.ItemStack buildUpgradeItem(Clan clan, UpgradeType type) {
        Material material;
        String name;
        String currentValue;

        switch (type) {
            case MEMBER_CAPACITY -> {
                material = Material.PLAYER_HEAD;
                name = "&b&lMember Capacity";
                currentValue = String.valueOf(clan.getMemberCapacity());
            }
            case BANK_CAPACITY -> {
                material = Material.GOLD_BLOCK;
                name = "&e&lBank Capacity";
                currentValue = plugin.getEconomyHook().format(clan.getBankCapacity());
            }
            case STORAGE_LEVEL -> {
                material = Material.CHEST;
                name = "&6&lStorage Level";
                currentValue = String.valueOf(clan.getStorageLevel());
            }
            case WARP_CAPACITY -> {
                material = Material.ENDER_PEARL;
                name = "&d&lWarp Slots";
                currentValue = String.valueOf(clan.getWarpCapacity());
            }
            case ALLIANCE_CAPACITY -> {
                material = Material.WHITE_BANNER;
                name = "&f&lAlliance Slots";
                currentValue = String.valueOf(clan.getAllianceCapacity());
            }
            default -> {
                material = Material.EXPERIENCE_BOTTLE;
                name = "&a&lClan Level";
                currentValue = String.valueOf(clan.getClanLevel());
            }
        }

        return new ItemBuilder(material)
                .name(name)
                .lore(
                        "&7Current: &f" + currentValue,
                        "",
                        "&eClick to upgrade!"
                ).build();
    }
}
