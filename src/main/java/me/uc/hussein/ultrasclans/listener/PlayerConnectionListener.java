package me.uc.hussein.ultrasclans.listener;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * عند دخول اللاعب: يُعاد بناء أي cooldown نشط من قاعدة البيانات إلى
 * الذاكرة، حتى لا يُلتف حولها بإعادة تشغيل السيرفر (قسم 54/84).
 */
public final class PlayerConnectionListener implements Listener {

    private final UltrasClansPlugin plugin;

    public PlayerConnectionListener(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        var clanService = plugin.getClanService();

        plugin.getMiningProgressManager().loadFor(uuid);
        plugin.getChatService().loadFor(uuid);
        plugin.getEventService().onPlayerReconnect(event.getPlayer());

        plugin.getScheduler().supplyAsync(() -> {
            long lastLeave = clanService.getCooldownRepository().getLastLeaveAt(uuid);
            long lastCreateAttempt = clanService.getCooldownRepository().getLastCreateAttemptAt(uuid);
            return new long[]{lastLeave, lastCreateAttempt};
        }).whenComplete((values, error) -> plugin.getScheduler().runSync(() -> {
            if (error != null || values == null) {
                return;
            }
            long rejoinCooldownMs = plugin.getConfigManager().get()
                    .getLong("membership.rejoin-cooldown-minutes", 10) * 60_000L;
            long createCooldownMs = plugin.getConfigManager().get()
                    .getLong("clan-creation.cooldown-seconds", 60) * 1000L;

            long rejoinExpiry = values[0] + rejoinCooldownMs;
            if (rejoinExpiry > System.currentTimeMillis()) {
                clanService.getRejoinCooldown().set(uuid, (rejoinExpiry - System.currentTimeMillis()) / 1000);
            }
            long createExpiry = values[1] + createCooldownMs;
            if (createExpiry > System.currentTimeMillis()) {
                clanService.getCreateCooldown().set(uuid, (createExpiry - System.currentTimeMillis()) / 1000);
            }
        }));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getGuiManager().pendingInput().clear(event.getPlayer().getUniqueId());
        plugin.getMiningProgressManager().flushAndUnload(event.getPlayer().getUniqueId());
        plugin.getChatService().unload(event.getPlayer().getUniqueId());
        plugin.getTpaService().clearFor(event.getPlayer().getUniqueId());
        plugin.getEventService().onPlayerDisconnect(event.getPlayer().getUniqueId());
    }
}
