package me.uc.hussein.ultrasclans.rank;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.ClanRankDefinition;
import me.uc.hussein.ultrasclans.model.ClanTier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * يحمّل القالب الافتراضي للرتب الداخلية (member-ranks) ومستويات الـCP
 * التلقائية (tiers) من ranks.yml، ويوفرها لأي كلان جديد أو لحساب الـTier.
 */
public final class RankTemplate {

    private final UltrasClansPlugin plugin;
    private final List<DefaultRank> defaultRanks = new ArrayList<>();
    private final List<ClanTier> tiers = new ArrayList<>();

    public record DefaultRank(String key, String name, String display, String color, int priority,
                               boolean protectedRank, boolean defaultRank, Set<String> permissions) {
    }

    public RankTemplate(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        defaultRanks.clear();
        tiers.clear();
        FileConfiguration yaml = plugin.getConfigManager().loadYaml("ranks.yml");

        List<?> memberRanksRaw = yaml.getList("member-ranks");
        if (memberRanksRaw != null) {
            for (Object obj : memberRanksRaw) {
                if (obj instanceof ConfigurationSection section) {
                    defaultRanks.add(readRank(section));
                } else if (obj instanceof java.util.Map<?, ?> map) {
                    org.bukkit.configuration.MemoryConfiguration mem = new org.bukkit.configuration.MemoryConfiguration();
                    for (var entry : map.entrySet()) {
                        mem.set(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    defaultRanks.add(readRank(mem));
                }
            }
        }

        List<?> tiersRaw = yaml.getList("tiers");
        if (tiersRaw != null) {
            for (Object obj : tiersRaw) {
                ConfigurationSection section = toSection(obj);
                if (section != null) {
                    tiers.add(new ClanTier(
                            section.getString("id", "unknown"),
                            section.getString("name", "Unknown"),
                            section.getString("display", "&7Unknown"),
                            section.getString("symbol", ""),
                            section.getLong("min-cp", 0),
                            section.getLong("max-cp", -1)
                    ));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ConfigurationSection toSection(Object obj) {
        if (obj instanceof ConfigurationSection section) {
            return section;
        }
        if (obj instanceof java.util.Map<?, ?> map) {
            org.bukkit.configuration.MemoryConfiguration mem = new org.bukkit.configuration.MemoryConfiguration();
            for (var entry : map.entrySet()) {
                mem.set(String.valueOf(entry.getKey()), entry.getValue());
            }
            return mem;
        }
        return null;
    }

    private DefaultRank readRank(ConfigurationSection section) {
        Set<String> permissions = new LinkedHashSet<>(section.getStringList("permissions"));
        return new DefaultRank(
                section.getString("id", "member"),
                section.getString("name", "Member"),
                section.getString("display", "&7Member"),
                section.getString("color", "&7"),
                section.getInt("priority", 1),
                section.getBoolean("protected", false),
                section.getBoolean("default-rank", false),
                permissions
        );
    }

    public List<ClanRankDefinition> buildDefaultRanksFor(UUID clanId) {
        List<ClanRankDefinition> result = new ArrayList<>();
        for (DefaultRank def : defaultRanks) {
            result.add(new ClanRankDefinition(
                    clanId, def.key(), def.name(), def.display(), def.color(),
                    def.priority(), def.protectedRank(), def.defaultRank(),
                    new LinkedHashSet<>(def.permissions())
            ));
        }
        return result;
    }

    public String getOwnerRankKey() {
        return defaultRanks.stream()
                .filter(r -> r.priority() >= 100 || r.key().equals("owner"))
                .map(DefaultRank::key)
                .findFirst()
                .orElse("owner");
    }

    public String getDefaultMemberRankKey() {
        return defaultRanks.stream()
                .filter(DefaultRank::defaultRank)
                .map(DefaultRank::key)
                .findFirst()
                .orElse("member");
    }

    public ClanTier getTierForCp(long cp) {
        for (ClanTier tier : tiers) {
            if (tier.matches(cp)) {
                return tier;
            }
        }
        return tiers.isEmpty() ? null : tiers.get(0);
    }

    public List<ClanTier> getTiers() {
        return tiers;
    }
}
