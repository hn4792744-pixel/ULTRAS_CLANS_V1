package me.uc.hussein.ultrasclans.points;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.repository.MiningProgressRepository;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * يتتبع عدّاد الكتل المكسورة تراكميًا لكل لاعب/خام في الذاكرة (لتفادي
 * ضغط قاعدة البيانات عند كل كتلة - قسم 59)، ويمنح CP عند بلوغ العتبة
 * المُعرَّفة في points.yml. يُحمَّل من قاعدة البيانات عند الدخول ويُحفظ
 * عند الخروج/إيقاف التشغيل حتى لا يُفقد التقدم (قسم 84).
 */
public final class MiningProgressManager {

    private final UltrasClansPlugin plugin;
    private final MiningProgressRepository repository;

    /** playerUuid -> (material name -> عدّاد تراكمي) */
    private final Map<UUID, Map<String, Integer>> cache = new ConcurrentHashMap<>();

    public MiningProgressManager(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.repository = new MiningProgressRepository(plugin.getDatabaseManager());
    }

    public void loadFor(UUID uuid) {
        plugin.getScheduler().supplyAsync(() -> repository.loadAll(uuid))
                .whenComplete((data, error) -> {
                    if (error == null) {
                        cache.put(uuid, new ConcurrentHashMap<>(data));
                    } else {
                        cache.put(uuid, new ConcurrentHashMap<>());
                    }
                });
    }

    public void flushAndUnload(UUID uuid) {
        Map<String, Integer> data = cache.remove(uuid);
        if (data == null || data.isEmpty()) {
            return;
        }
        plugin.getScheduler().runAsync(() -> repository.saveAll(uuid, data));
    }

    public void flushAll() {
        for (var entry : cache.entrySet()) {
            try {
                repository.saveAll(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to flush mining progress for " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    /**
     * يسجل كسر كتلة خام واحدة، ويمنح CP للكلان إذا بلغ العدّاد التراكمي
     * عتبة جديدة (blocks-per-cp). لا يفعل شيئًا إذا الخام غير مفعّل أو
     * اللاعب بلا كلان.
     */
    public void recordBlockBreak(Player player, String materialName) {
        var config = plugin.getPointsService().getConfig();
        if (!config.getBoolean("mining.enabled", true)) {
            return;
        }
        var oreSection = config.getConfigurationSection("mining.ores." + materialName);
        if (oreSection == null || !oreSection.getBoolean("enabled", false)) {
            return;
        }
        int blocksPerCp = oreSection.getInt("blocks-per-cp", 0);
        if (blocksPerCp <= 0) {
            return;
        }

        var clanOpt = plugin.getClanService().getPlayerClan(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            return;
        }
        Clan clan = clanOpt.get();

        Map<String, Integer> playerData = cache.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        int newCount = playerData.merge(materialName, 1, Integer::sum);

        if (newCount % blocksPerCp == 0) {
            String displayName = materialName.replace("_", " ").toLowerCase();
            plugin.getPointsService().addCp(player, clan, 1, "Mining (" + displayName + ")");
        }
    }
}
