package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.ConfirmGuiHolder;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class ConfirmGui {

    public static final int SIZE = 27;
    public static final int SLOT_YES = 11;
    public static final int SLOT_NO = 15;

    private final UltrasClansPlugin plugin;

    public ConfirmGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, java.util.UUID clanId, ConfirmGuiHolder.Action action, int contextId, String question) {
        open(player, clanId, action, contextId, null, question);
    }

    public void open(Player player, java.util.UUID clanId, ConfirmGuiHolder.Action action, int contextId,
                      java.util.UUID contextUuid, String question) {
        ConfirmGuiHolder holder = new ConfirmGuiHolder(player.getUniqueId(), clanId, action, contextId, contextUuid);
        String title = plugin.getMessageService().text(player, "general.gui-title-confirm");
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        inv.setItem(13, new ItemBuilder(Material.PAPER).name(question).build());
        inv.setItem(SLOT_YES, new ItemBuilder(Material.LIME_WOOL).name("&a&lYes").build());
        inv.setItem(SLOT_NO, new ItemBuilder(Material.RED_WOOL).name("&c&lNo").build());

        player.openInventory(inv);
    }
}
