package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.MemberInfoGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanMember;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import me.uc.hussein.ultrasclans.model.ClanRankDefinition;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

public final class MemberInfoGui {

    public static final int SIZE = 27;
    public static final int SLOT_KICK = 11;
    public static final int SLOT_PROMOTE = 13;
    public static final int SLOT_DEMOTE = 15;
    public static final int SLOT_BACK = 22;

    private final UltrasClansPlugin plugin;

    public MemberInfoGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, Clan clan, ClanMember target) {
        MemberInfoGuiHolder holder = new MemberInfoGuiHolder(viewer.getUniqueId(), clan.getId(), target.getUuid());
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(target.getUuid());
        String title = plugin.getMessageService().text(viewer, "general.gui-title-member-info",
                java.util.Map.of("player", targetPlayer.getName() != null ? targetPlayer.getName() : "Unknown"));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        Optional<ClanRankDefinition> rank = plugin.getClanService().getRank(clan.getId(), target.getRankKey());
        String joinedDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date(target.getJoinedAt()));
        boolean isOwner = clan.getOwnerUuid().equals(target.getUuid());

        inv.setItem(4, ItemBuilder.skullOf(targetPlayer)
                .name("&f" + (targetPlayer.getName() != null ? targetPlayer.getName() : "Unknown"))
                .lore(
                        "&7Rank: " + rank.map(ClanRankDefinition::getDisplay).orElse("&7Member"),
                        "&7Level: &f" + target.getMemberLevel(),
                        "&7XP: &f" + target.getMemberXp(),
                        "&7Kills: &f" + target.getKills() + " &7Deaths: &f" + target.getDeaths(),
                        "&7Contribution: &f" + target.getContribution(),
                        "&7Joined: &f" + joinedDate,
                        "&7Online: " + (targetPlayer.isOnline() ? "&aYes" : "&7No")
                ).build());

        boolean canManage = plugin.getPermissionService().hasPermission(viewer.getUniqueId(), ClanPermission.CLAN_KICK)
                || plugin.getPermissionService().isOwner(viewer.getUniqueId());

        if (!isOwner && canManage && !viewer.getUniqueId().equals(target.getUuid())) {
            inv.setItem(SLOT_KICK, new ItemBuilder(Material.BARRIER)
                    .name("&c&lKick").lore("&7Remove this player from the clan.").build());
        }

        boolean canPromote = plugin.getPermissionService().hasPermission(viewer.getUniqueId(), ClanPermission.CLAN_PROMOTE)
                || plugin.getPermissionService().isOwner(viewer.getUniqueId());
        if (!isOwner && canPromote) {
            inv.setItem(SLOT_PROMOTE, new ItemBuilder(Material.LIME_DYE)
                    .name("&a&lPromote").lore("&7Move to the next rank up.").build());
            inv.setItem(SLOT_DEMOTE, new ItemBuilder(Material.GRAY_DYE)
                    .name("&7&lDemote").lore("&7Move to the next rank down.").build());
        }

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(viewer, "general.back-button")).build());

        viewer.openInventory(inv);
    }
}
