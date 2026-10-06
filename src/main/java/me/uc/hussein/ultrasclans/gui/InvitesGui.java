package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.InvitesGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanInvite;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public final class InvitesGui {

    public static final int SIZE = 54;
    public static final int SLOT_BACK = 49;

    private final UltrasClansPlugin plugin;

    public InvitesGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer) {
        plugin.getInviteService().listPendingForPlayer(viewer.getUniqueId()).whenComplete((invites, error) ->
                plugin.getScheduler().runSync(() -> render(viewer, error == null ? invites : List.of())));
    }

    private void render(Player viewer, List<ClanInvite> invites) {
        InvitesGuiHolder holder = new InvitesGuiHolder(viewer.getUniqueId());
        holder.setCurrentEntries(invites);
        String title = plugin.getMessageService().text(viewer, "general.gui-title-invites");
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        int slot = 0;
        for (ClanInvite invite : invites) {
            if (slot >= 45) break;
            inv.setItem(slot++, buildInviteItem(invite));
        }

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(viewer, "general.back-button")).build());

        viewer.openInventory(inv);
    }

    private ItemStack buildInviteItem(ClanInvite invite) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(invite.getClanId());
        String clanName = clanOpt.map(Clan::getName).orElse("Unknown Clan");
        return new ItemBuilder(Material.PAPER)
                .name("&f" + clanName)
                .lore(
                        "&7You were invited to join.",
                        "",
                        "&aLeft-click to accept",
                        "&cRight-click to deny"
                ).build();
    }
}
