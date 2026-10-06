package me.uc.hussein.ultrasclans.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanMember;
import me.uc.hussein.ultrasclans.model.ClanRankDefinition;
import me.uc.hussein.ultrasclans.util.ColorUtil;
import org.bukkit.OfflinePlayer;

import java.util.Optional;

/**
 * %ultras_clan_name% وباقي المتغيرات (قسم 47). تقرأ من الكاش فقط (لا قاعدة
 * بيانات ولا Bukkit API ثقيل)، فهي آمنة للاستدعاء المتكرر من أي بلجن.
 * لا يُوثق بقيمها في أي قرار داخل البلاجن نفسه (قسم 89).
 */
public final class UltrasPlaceholderExpansion extends PlaceholderExpansion {

    private final UltrasClansPlugin plugin;

    public UltrasPlaceholderExpansion(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "ultras";
    }

    @Override
    public String getAuthor() {
        return "UC_Hussein";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String empty = plugin.getConfigManager().get().getString("placeholders.empty-value", "");
        if (player == null || plugin.getClanService() == null) {
            return empty;
        }
        Optional<ClanMember> memberOpt = plugin.getClanService().getMember(player.getUniqueId());
        if (memberOpt.isEmpty()) {
            return empty;
        }
        ClanMember member = memberOpt.get();
        Optional<Clan> clanOpt = plugin.getClanService().getClan(member.getClanId());
        if (clanOpt.isEmpty()) {
            return empty;
        }
        Clan clan = clanOpt.get();

        return switch (params.toLowerCase()) {
            case "clan_name", "clan_tag" -> clan.getName();
            case "clan_color" -> colorCode(clan.getColor());
            case "clan_rank" -> {
                var tier = plugin.getRankTemplate().getTierForCp(clan.getCp());
                yield tier != null ? ColorUtil.colorize(tier.getDisplay()) : empty;
            }
            case "clan_rank_name" -> {
                var tier = plugin.getRankTemplate().getTierForCp(clan.getCp());
                yield tier != null ? tier.getName() : empty;
            }
            case "clan_cp" -> String.valueOf(clan.getCp());
            case "clan_level" -> String.valueOf(clan.getClanLevel());
            case "clan_members" -> String.valueOf(plugin.getClanService().getMembers(clan.getId()).size());
            case "clan_online" -> String.valueOf(plugin.getClanService().getMembers(clan.getId()).stream()
                    .filter(m -> plugin.getServer().getPlayer(m.getUuid()) != null).count());
            case "clan_bank" -> String.format("%.2f", clan.getBankBalance());
            case "clan_bank_capacity" -> String.format("%.2f", clan.getBankCapacity());
            case "clan_storage_level" -> String.valueOf(clan.getStorageLevel());
            case "clan_warps" -> String.valueOf(clan.getWarpCapacity());
            case "clan_alliances" -> String.valueOf(plugin.getAllianceService().getAlliances(clan.getId()).size());
            case "clan_member_level" -> String.valueOf(member.getMemberLevel());
            case "clan_member_xp" -> String.valueOf(member.getMemberXp());
            case "clan_member_kills" -> String.valueOf(member.getKills());
            case "clan_member_deaths" -> String.valueOf(member.getDeaths());
            case "clan_member_contribution" -> String.valueOf(member.getContribution());
            case "clan_role" -> plugin.getClanService().getRank(clan.getId(), member.getRankKey())
                    .map(ClanRankDefinition::getDisplay).map(ColorUtil::colorize).orElse(empty);
            case "clan_owner" -> {
                String name = plugin.getServer().getOfflinePlayer(clan.getOwnerUuid()).getName();
                yield name != null ? name : empty;
            }
            default -> null;
        };
    }

    private String colorCode(String bukkitName) {
        try {
            return org.bukkit.ChatColor.valueOf(bukkitName.toUpperCase()).toString();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
