package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.AllianceSettingsGuiHolder;
import me.uc.hussein.ultrasclans.model.AllianceSettings;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class AllianceSettingsGui {

    public static final int SIZE = 27;
    public static final int SLOT_SPAWN = 9;
    public static final int SLOT_WARP = 10;
    public static final int SLOT_STORAGE = 11;
    public static final int SLOT_BANK = 12;
    public static final int SLOT_MEMBER_LIST = 13;
    public static final int SLOT_CLAN_INFO = 14;
    public static final int SLOT_PREVENT_PVP = 15;
    public static final int SLOT_BACK = 22;

    private final UltrasClansPlugin plugin;

    public AllianceSettingsGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan, java.util.UUID allianceId, java.util.UUID otherClanId) {
        AllianceSettingsGuiHolder holder = new AllianceSettingsGuiHolder(player.getUniqueId(), clan.getId(),
                allianceId, otherClanId);
        String title = plugin.getMessageService().text(player, "general.gui-title-alliance-settings", Map.of("clan", clan.getName()));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        AllianceSettings settings = plugin.getAllianceService().getSettings(allianceId, clan.getId());

        inv.setItem(SLOT_SPAWN, toggleItem(Material.ENDER_EYE, "Allow Spawn", settings.isAllowSpawn()));
        inv.setItem(SLOT_WARP, toggleItem(Material.ENDER_PEARL, "Allow Warps", settings.isAllowWarp()));
        inv.setItem(SLOT_STORAGE, toggleItem(Material.BARREL, "Allow Storage", settings.isAllowStorage()));
        inv.setItem(SLOT_BANK, toggleItem(Material.GOLD_INGOT, "Allow Bank Deposit", settings.isAllowBank()));
        inv.setItem(SLOT_MEMBER_LIST, toggleItem(Material.BOOK, "Allow Member List", settings.isAllowMemberList()));
        inv.setItem(SLOT_CLAN_INFO, toggleItem(Material.PAPER, "Allow Clan Info", settings.isAllowClanInfo()));
        inv.setItem(SLOT_PREVENT_PVP, toggleItem(Material.SHIELD, "Prevent PvP", settings.isPreventPvp()));

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "general.back-button")).build());

        player.openInventory(inv);
    }

    private ItemStack toggleItem(Material material, String name, boolean enabled) {
        return new ItemBuilder(material)
                .name((enabled ? "&a&l" : "&7&l") + name + (enabled ? " &a[ON]" : " &7[OFF]"))
                .lore("&7Click to toggle.").build();
    }
}
