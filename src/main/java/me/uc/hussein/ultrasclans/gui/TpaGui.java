package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.TpaGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class TpaGui {

    public static final int SIZE = 27;

    private final UltrasClansPlugin plugin;

    public TpaGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        List<Player> online = new ArrayList<>();
        for (var member : plugin.getClanService().getMembers(clan.getId())) {
            if (member.getUuid().equals(player.getUniqueId())) continue;
            Player p = plugin.getServer().getPlayer(member.getUuid());
            if (p != null) online.add(p);
        }

        TpaGuiHolder holder = new TpaGuiHolder(player.getUniqueId());
        holder.setCurrentEntries(online);
        String title = plugin.getMessageService().text(player, "general.gui-title-tpa");
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        for (int i = 0; i < online.size() && i < 18; i++) {
            Player target = online.get(i);
            inv.setItem(i, ItemBuilder.skullOf(target)
                    .name("&f" + target.getName())
                    .lore("&aOnline", "", "&eClick for options!").build());
        }

        player.openInventory(inv);
    }
}
