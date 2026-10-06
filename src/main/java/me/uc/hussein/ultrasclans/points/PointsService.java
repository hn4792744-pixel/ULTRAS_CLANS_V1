package me.uc.hussein.ultrasclans.points;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanTier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * الخدمة المركزية الوحيدة المخوّلة بتعديل CP لأي كلان (قسم 9).
 * أي مصدر CP (قتل، تعدين، إيداع بنك...) يمر حصريًا عبر addCp هنا، وأي
 * إنفاق CP (ترقيات) يمر عبر spendCp - لا يُعدَّل clan.cp من أي مكان آخر.
 */
public final class PointsService {

    private final UltrasClansPlugin plugin;
    private FileConfiguration config;

    /** uuid -> [بداية اليوم الحالي بالمللي ثانية, الكمية المستهلكة اليوم] */
    private final Map<UUID, long[]> dailyUsage = new ConcurrentHashMap<>();

    public PointsService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.config = plugin.getConfigManager().loadYaml("points.yml");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * يمنح CP لكلان لاعب معيّن، بعد تطبيق الحد اليومي لكل لاعب. آمن
     * للاستدعاء من أي thread طالما تم استدعاؤه بعد جلب الكلان من الكاش.
     * يُحدّث الكاش فوريًا (synchronized على الكلان) ثم يحفظ لاحقًا بأمان.
     *
     * @return الكمية الفعلية الممنوحة بعد قصّ الحد اليومي (قد تكون 0)
     */
    public long addCp(Player actor, Clan clan, long rawAmount, String sourceKey) {
        if (rawAmount <= 0 || clan == null) {
            return 0;
        }

        long allowed = clampToDailyCap(actor.getUniqueId(), rawAmount);
        if (allowed <= 0) {
            return 0;
        }

        ClanTier oldTier;
        ClanTier newTier;
        long newCp;
        synchronized (clan) {
            long oldCp = clan.getCp();
            newCp = oldCp + allowed;
            oldTier = plugin.getRankTemplate().getTierForCp(oldCp);
            newTier = plugin.getRankTemplate().getTierForCp(newCp);
            clan.setCp(newCp);
        }

        persistAndNotify(clan, oldTier, newTier, actor);

        plugin.getMessageService().send(actor, "cp.gained", Map.of(
                "amount", String.valueOf(allowed),
                "source", sourceKey
        ));
        plugin.playSound(actor, "cp_gain");

        return allowed;
    }

    /**
     * يمنح CP للكلان مباشرة بدون حد يومي لكل لاعب - يُستخدم فقط لمكافآت
     * على مستوى الكلان بالكامل (مثل إكمال مهمة جماعية) وليست نتاج فعل
     * لاعب واحد قابل للاستغلال. لا يُستدعى أبدًا من مصادر CP الفردية.
     */
    public void addCpForClan(Clan clan, long amount, String sourceKey) {
        if (amount <= 0 || clan == null) {
            return;
        }
        ClanTier oldTier;
        ClanTier newTier;
        synchronized (clan) {
            long oldCp = clan.getCp();
            long newCp = oldCp + amount;
            oldTier = plugin.getRankTemplate().getTierForCp(oldCp);
            newTier = plugin.getRankTemplate().getTierForCp(newCp);
            clan.setCp(newCp);
        }
        persistAndNotify(clan, oldTier, newTier, null);

        for (var member : plugin.getClanService().getMembers(clan.getId())) {
            Player online = plugin.getServer().getPlayer(member.getUuid());
            if (online != null) {
                plugin.getMessageService().send(online, "cp.gained", Map.of(
                        "amount", String.valueOf(amount), "source", sourceKey));
            }
        }
    }

    /**
     * يخصم CP من كلان (يُستخدم من UpgradeService). لا يُطبَّق عليه حد
     * يومي (الإنفاق ليس مصدر استغلال). يفشل بأمان إذا كان الرصيد غير كافٍ.
     *
     * @return true إذا تم الخصم بنجاح
     */
    public boolean spendCp(Clan clan, long amount) {
        if (amount <= 0) {
            return false;
        }
        ClanTier oldTier;
        ClanTier newTier;
        boolean success;
        synchronized (clan) {
            long oldCp = clan.getCp();
            if (oldCp < amount) {
                return false;
            }
            long newCp = oldCp - amount;
            oldTier = plugin.getRankTemplate().getTierForCp(oldCp);
            newTier = plugin.getRankTemplate().getTierForCp(newCp);
            clan.setCp(newCp);
            success = true;
        }
        if (success) {
            persistAndNotify(clan, oldTier, newTier, null);
        }
        return success;
    }

    private void persistAndNotify(Clan clan, ClanTier oldTier, ClanTier newTier, Player triggeringActor) {
        plugin.getScheduler().runAsync(() -> plugin.getClanService().getClanRepository().update(clan));

        boolean tierChanged = (oldTier == null) != (newTier == null)
                || (oldTier != null && newTier != null && !oldTier.getId().equals(newTier.getId()));
        if (!tierChanged || newTier == null) {
            return;
        }
        boolean tierUp = oldTier == null || newTier.getMinCp() > oldTier.getMinCp();
        String messageKey = tierUp ? "cp.tier-up" : "cp.tier-down";

        for (var member : plugin.getClanService().getMembers(clan.getId())) {
            Player online = plugin.getServer().getPlayer(member.getUuid());
            if (online == null) {
                continue;
            }
            plugin.getMessageService().send(online, messageKey,
                    Map.of("tier", stripColorSafe(newTier.getDisplay())));
            if (tierUp) {
                plugin.playSound(online, "rankup");
            }
        }
    }

    private String stripColorSafe(String input) {
        return me.uc.hussein.ultrasclans.util.ColorUtil.colorize(input);
    }

    private long clampToDailyCap(UUID playerUuid, long requested) {
        long cap = config.getLong("daily-cp-cap-per-player", 500);
        if (cap <= 0) {
            return requested; // 0 = بلا حد
        }
        long now = System.currentTimeMillis();
        long[] usage = dailyUsage.computeIfAbsent(playerUuid, k -> new long[]{now, 0});
        synchronized (usage) {
            if (now - usage[0] > 24L * 60L * 60L * 1000L) {
                usage[0] = now;
                usage[1] = 0;
            }
            long remaining = cap - usage[1];
            if (remaining <= 0) {
                return 0;
            }
            long granted = Math.min(remaining, requested);
            usage[1] += granted;
            return granted;
        }
    }
}
