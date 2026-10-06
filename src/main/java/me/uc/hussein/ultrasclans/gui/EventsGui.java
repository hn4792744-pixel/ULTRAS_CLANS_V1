package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.event.ClanEvent;
import me.uc.hussein.ultrasclans.gui.holder.EventsGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EventsGui {

    public static final int SIZE = 54;
    public static final int SLOT_CHALLENGE = 49;
    public static final int SLOT_BACK = 53;

    private final UltrasClansPlugin plugin;

    public EventsGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        List<ClanEvent> live = plugin.getEventService().getActiveOrPendingForClan(clan.getId());
        plugin.getEventService().getRecentFinished(clan.getId()).whenComplete((finished, error) ->
                plugin.getScheduler().runSync(() ->
                        render(player, clan, live, error == null ? finished : List.of())));
    }

    private void render(Player player, Clan clan, List<ClanEvent> live, List<ClanEvent> finished) {
        EventsGuiHolder holder = new EventsGuiHolder(player.getUniqueId(), clan.getId());
        List<ClanEvent> all = new ArrayList<>(live);
        all.addAll(finished);
        holder.setCurrentEntries(all);

        String title = plugin.getMessageService().text(player, "general.gui-title-events", Map.of("clan", clan.getName()));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        int slot = 0;
        for (ClanEvent event : all) {
            if (slot >= 45) break;
            Clan other = plugin.getClanService().getClan(event.otherClan(clan.getId())).orElse(null);
            String otherName = other != null ? other.getName() : "Unknown";
            Material material = switch (event.getStatus()) {
                case PENDING_INVITE -> Material.PAPER;
                case ACCEPTED_PREPARING, ACTIVE -> Material.DIAMOND_SWORD;
                case FINISHED -> Material.GOLD_INGOT;
                case CANCELLED -> Material.BARRIER;
            };
            boolean incoming = event.getStatus().name().equals("PENDING_INVITE") && event.getClanB().equals(clan.getId());
            inv.setItem(slot++, new ItemBuilder(material)
                    .name("&f" + otherName + " &8- &e" + event.getStatus().name())
                    .lore(
                            "&7Score: &f" + event.getScoreA() + "-" + event.getScoreB(),
                            "&7Rounds: &f" + event.getTotalRounds(),
                            "&7Wager: &f" + plugin.getEconomyHook().format(event.getMoneyWager())
                                    + " &7+ &d" + event.getCpWager() + " CP",
                            incoming ? "" : "",
                            incoming ? "&aLeft-click to accept" : "",
                            incoming ? "&cRight-click to decline" : ""
                    ).build());
        }

        inv.setItem(SLOT_CHALLENGE, new ItemBuilder(Material.DIAMOND_SWORD)
                .name("&c&lChallenge a Clan")
                .lore("&7Pick a clan and set up a battle.", "", "&eClick to browse!").build());
        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "general.back-button")).build());

        player.openInventory(inv);
    }
}
