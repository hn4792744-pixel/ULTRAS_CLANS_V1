package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.MembersGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanMember;
import me.uc.hussein.ultrasclans.model.ClanRankDefinition;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MembersGui {

    public static final int SIZE = 54;
    private static final int PAGE_SIZE = 45;
    public static final int SLOT_PREV = 45;
    public static final int SLOT_BACK = 49;
    public static final int SLOT_NEXT = 53;

    private final UltrasClansPlugin plugin;

    public MembersGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, Clan clan, int page) {
        List<ClanMember> all = new ArrayList<>(plugin.getClanService().getMembers(clan.getId()));
        all.sort((a, b) -> {
            Optional<ClanRankDefinition> ra = plugin.getClanService().getRank(clan.getId(), a.getRankKey());
            Optional<ClanRankDefinition> rb = plugin.getClanService().getRank(clan.getId(), b.getRankKey());
            int pa = ra.map(ClanRankDefinition::getPriority).orElse(0);
            int pb = rb.map(ClanRankDefinition::getPriority).orElse(0);
            return Integer.compare(pb, pa);
        });

        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PAGE_SIZE;
        int to = Math.min(all.size(), from + PAGE_SIZE);
        List<ClanMember> pageEntries = all.subList(from, to);

        MembersGuiHolder holder = new MembersGuiHolder(viewer.getUniqueId(), clan.getId(), safePage);
        holder.setCurrentPageEntries(new ArrayList<>(pageEntries));
        String title = plugin.getMessageService().text(viewer, "general.gui-title-members",
                java.util.Map.of("clan", clan.getName()));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        int slot = 0;
        for (ClanMember member : pageEntries) {
            inv.setItem(slot++, buildMemberItem(clan, member));
        }

        if (safePage > 0) {
            inv.setItem(SLOT_PREV, new ItemBuilder(Material.ARROW).name("&7« Previous Page").build());
        }
        if (safePage < totalPages - 1) {
            inv.setItem(SLOT_NEXT, new ItemBuilder(Material.ARROW).name("&7Next Page »").build());
        }
        inv.setItem(SLOT_BACK, new ItemBuilder(Material.BARRIER)
                .name(plugin.getMessageService().text(viewer, "general.back-button")).build());

        viewer.openInventory(inv);
    }

    private ItemStack buildMemberItem(Clan clan, ClanMember member) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(member.getUuid());
        boolean online = target.isOnline();
        Optional<ClanRankDefinition> rank = plugin.getClanService().getRank(clan.getId(), member.getRankKey());
        String rankDisplay = rank.map(ClanRankDefinition::getDisplay).orElse("&7Member");
        boolean isOwner = clan.getOwnerUuid().equals(member.getUuid());

        String statusKey = online ? "members.online" : "members.offline";
        return ItemBuilder.skullOf(target)
                .name((target.getName() != null ? target.getName() : "Unknown") + " &8" + (isOwner ? "&4[Owner]" : ""))
                .lore(
                        "&7Rank: " + rankDisplay,
                        "&7Level: &f" + member.getMemberLevel(),
                        "&7Kills: &f" + member.getKills() + " &7Deaths: &f" + member.getDeaths(),
                        "&7Status: " + plugin.getMessageManager().get(statusKey),
                        "",
                        "&eClick for details!"
                ).build();
    }
}
