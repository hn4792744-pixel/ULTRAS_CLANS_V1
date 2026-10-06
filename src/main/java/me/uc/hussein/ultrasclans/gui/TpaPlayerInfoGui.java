package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.TpaPlayerInfoGuiHolder;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class TpaPlayerInfoGui {

    public static final int SIZE = 27;
    public static final int SLOT_SEND_TPA = 11;
    public static final int SLOT_REQUEST_HERE = 15;
    public static final int SLOT_BACK = 22;

    private final UltrasClansPlugin plugin;

    public TpaPlayerInfoGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, Player target) {
        TpaPlayerInfoGuiHolder holder = new TpaPlayerInfoGuiHolder(viewer.getUniqueId(), target.getUniqueId());
        String title = plugin.getMessageService().text(viewer, "general.gui-title-tpa-player",
                java.util.Map.of("player", target.getName()));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        inv.setItem(4, ItemBuilder.skullOf(target).name("&f" + target.getName()).build());
        inv.setItem(SLOT_SEND_TPA, new ItemBuilder(Material.ENDER_PEARL)
                .name("&a&lSend TPA")
                .lore("&7Teleport to " + target.getName() + ".").build());
        inv.setItem(SLOT_REQUEST_HERE, new ItemBuilder(Material.ENDER_EYE)
                .name("&b&lRequest Player To Me")
                .lore("&7Ask " + target.getName() + " to teleport to you.").build());
        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(viewer, "general.back-button")).build());

        viewer.openInventory(inv);
    }
}
