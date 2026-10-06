package me.uc.hussein.ultrasclans.teleport;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * منطق التنقل المؤجل المشترك بين Warp وSpawn وTPA (أقسام 22/23/26):
 * عدّ تنازلي فوق القلوب، إلغاء عند الحركة/الضرر/تغيير العالم، وحماية
 * قصيرة بعد الوصول.
 */
public final class TeleportService {

    private record PendingTeleport(Location startLocation, BukkitTask task) {
    }

    private final UltrasClansPlugin plugin;
    private final Map<UUID, PendingTeleport> active = new ConcurrentHashMap<>();
    private final Map<UUID, Long> arrivalProtection = new ConcurrentHashMap<>();

    public TeleportService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isTeleporting(UUID uuid) {
        return active.containsKey(uuid);
    }

    public boolean isProtected(UUID uuid) {
        Long until = arrivalProtection.get(uuid);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            arrivalProtection.remove(uuid);
            return false;
        }
        return true;
    }

    public void start(Player player, Location destination, long delaySeconds, String messageCategory) {
        UUID uuid = player.getUniqueId();
        if (active.containsKey(uuid)) {
            plugin.getMessageService().send(player, "warps.already-teleporting");
            return;
        }

        Location start = player.getLocation();
        plugin.playSound(player, "teleport_start");

        if (delaySeconds <= 0) {
            finish(player, destination);
            return;
        }

        int[] remaining = {(int) delaySeconds};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (remaining[0] <= 0) {
                active.remove(uuid);
                finish(player, destination);
                return;
            }
            if (plugin.getPrefsService().get(uuid).hud()) {
                Component actionBar = LegacyComponentSerializer.legacyAmpersand().deserialize(
                        plugin.getMessageService().raw(player, messageCategory + ".countdown")
                                .replace("{seconds}", String.valueOf(remaining[0])));
                player.sendActionBar(actionBar);
            }
            remaining[0]--;
        }, 0L, 20L);

        active.put(uuid, new PendingTeleport(start, task));
    }

    private void finish(Player player, Location destination) {
        player.teleport(destination);
        arrivalProtection.put(player.getUniqueId(),
                System.currentTimeMillis() + protectionSeconds() * 1000L);
        plugin.playSound(player, "teleport_success");
        plugin.getMessageService().send(player, "warps.success");
    }

    private long protectionSeconds() {
        return plugin.getConfigManager().loadYaml("warps.yml").getLong("protection-seconds", 2);
    }

    /** يُستدعى من مستمعي الحركة/الضرر/تغيير العالم. لا يفعل شيئًا إن لم يكن هناك نقل معلّق. */
    public void cancel(Player player, String reasonMessageKey) {
        PendingTeleport pending = active.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }
        pending.task().cancel();
        plugin.getMessageService().send(player, reasonMessageKey);
        plugin.playSound(player, "teleport_cancel");
    }

    public Location getStartLocation(UUID uuid) {
        PendingTeleport pending = active.get(uuid);
        return pending != null ? pending.startLocation() : null;
    }

    /** يُستدعى عند الخروج المفاجئ لتفادي تسريب المهمة المجدولة. */
    public void cancelSilently(UUID uuid) {
        PendingTeleport pending = active.remove(uuid);
        if (pending != null) {
            pending.task().cancel();
        }
    }
}
