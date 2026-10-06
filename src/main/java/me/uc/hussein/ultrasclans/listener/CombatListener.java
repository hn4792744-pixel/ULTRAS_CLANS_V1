package me.uc.hussein.ultrasclans.listener;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import me.uc.hussein.ultrasclans.task.TaskType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * يمنح CP لكلان القاتل عند قتل لاعب آخر، مع حماية كاملة من تبادل
 * القتل المتكرر بين نفس اللاعبين خلال نافذة زمنية (قسم 9).
 */
public final class CombatListener implements Listener {

    private final UltrasClansPlugin plugin;

    /** "killerUuid:victimUuid" -> [بداية النافذة, عدد القتلات المحتسبة فيها] */
    private final Map<String, long[]> pairWindows = new ConcurrentHashMap<>();

    public CombatListener(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        plugin.getClanService().getMember(victim.getUniqueId()).ifPresent(m -> {
            m.setDeaths(m.getDeaths() + 1);
            plugin.getScheduler().runAsync(() -> plugin.getClanService().getMemberRepository().update(m));
        });
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return; // موت طبيعي أو انتحار - لا CP
        }

        var config = plugin.getPointsService().getConfig();
        if (!config.getBoolean("kill-player.enabled", true)) {
            return;
        }

        plugin.getClanService().getMember(killer.getUniqueId()).ifPresent(m -> {
            m.setKills(m.getKills() + 1);
            plugin.getScheduler().runAsync(() -> plugin.getClanService().getMemberRepository().update(m));
        });

        Optional<Clan> killerClanOpt = plugin.getClanService().getPlayerClan(killer.getUniqueId());
        if (killerClanOpt.isEmpty()) {
            return; // القاتل بلا كلان - لا CP (قسم 9)
        }
        Clan killerClan = killerClanOpt.get();

        Optional<Clan> victimClanOpt = plugin.getClanService().getPlayerClan(victim.getUniqueId());
        if (victimClanOpt.isPresent() && victimClanOpt.get().getId().equals(killerClan.getId())) {
            long sameClanCp = config.getLong("kill-player.same-clan-kill-cp", 0);
            if (sameClanCp > 0) {
                plugin.getPointsService().addCp(killer, killerClan, sameClanCp, "Friendly Kill");
            }
            return;
        }

        long cp = computeKillCp(killer.getUniqueId(), victim.getUniqueId(), config);
        if (cp > 0) {
            plugin.getPointsService().addCp(killer, killerClan, cp, "PvP Kill");
        }
        plugin.getTaskService().incrementProgress(killerClan, TaskType.KILL_PLAYERS, 1);
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            return; // موت اللاعبين يُعالَج في onDeath أعلاه
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        plugin.getClanService().getPlayerClan(killer.getUniqueId())
                .ifPresent(clan -> plugin.getTaskService().incrementProgress(clan, TaskType.KILL_MOBS, 1));
    }

    private long computeKillCp(UUID killerUuid, UUID victimUuid, org.bukkit.configuration.file.FileConfiguration config) {
        long windowMs = config.getLong("kill-player.window-seconds", 600) * 1000L;
        long maxCounted = config.getLong("kill-player.max-counted-kills-in-window", 2);
        long firstKillCp = config.getLong("kill-player.first-kill-cp", 10);
        long repeatKillCp = config.getLong("kill-player.repeat-kill-cp", 2);

        String key = killerUuid + ":" + victimUuid;
        long now = System.currentTimeMillis();
        long[] window = pairWindows.computeIfAbsent(key, k -> new long[]{now, 0});

        synchronized (window) {
            if (now - window[0] > windowMs) {
                window[0] = now;
                window[1] = 0;
            }
            window[1]++;
            long countInWindow = window[1];
            if (countInWindow == 1) {
                return firstKillCp;
            } else if (countInWindow <= maxCounted) {
                return repeatKillCp;
            } else {
                return 0;
            }
        }
    }
}
