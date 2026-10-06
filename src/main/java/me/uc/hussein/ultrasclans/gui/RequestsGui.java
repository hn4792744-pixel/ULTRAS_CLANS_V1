package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.RequestsGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.JoinRequest;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class RequestsGui {

    public static final int SIZE = 54;
    public static final int SLOT_BACK = 49;

    private final UltrasClansPlugin plugin;

    public RequestsGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, Clan clan) {
        plugin.getRequestService().listPendingForClan(clan.getId()).whenComplete((requests, error) ->
                plugin.getScheduler().runSync(() -> render(viewer, clan, error == null ? requests : List.of())));
    }

    private void render(Player viewer, Clan clan, List<JoinRequest> requests) {
        RequestsGuiHolder holder = new RequestsGuiHolder(viewer.getUniqueId(), clan.getId());
        holder.setCurrentEntries(requests);
        String title = plugin.getMessageService().text(viewer, "general.gui-title-requests");
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        int slot = 0;
        for (JoinRequest request : requests) {
            if (slot >= 45) break;
            inv.setItem(slot++, buildRequestItem(request));
        }

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(viewer, "general.back-button")).build());

        viewer.openInventory(inv);
    }

    private ItemStack buildRequestItem(JoinRequest request) {
        OfflinePlayer requester = Bukkit.getOfflinePlayer(request.getRequesterUuid());
        return ItemBuilder.skullOf(requester)
                .name("&f" + (requester.getName() != null ? requester.getName() : "Unknown"))
                .lore(
                        "&7Requested to join your clan.",
                        "",
                        "&aLeft-click to accept",
                        "&cRight-click to deny"
                ).build();
    }
}
