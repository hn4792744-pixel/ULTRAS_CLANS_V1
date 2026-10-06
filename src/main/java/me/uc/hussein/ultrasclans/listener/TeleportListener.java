package me.uc.hussein.ultrasclans.listener;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class TeleportListener implements Listener {

    private final UltrasClansPlugin plugin;

    public TeleportListener(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getTeleportService().isTeleporting(event.getPlayer().getUniqueId())) {
            return;
        }
        // نتجاهل حركات النظر البحتة (تدوير الرأس فقط بدون تغيير الإحداثيات)
        if (event.getFrom().getX() == event.getTo().getX()
                && event.getFrom().getY() == event.getTo().getY()
                && event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }
        plugin.getTeleportService().cancel(event.getPlayer(), "warps.cancelled-move");
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (plugin.getTeleportService().isTeleporting(event.getPlayer().getUniqueId())) {
            plugin.getTeleportService().cancel(event.getPlayer(), "warps.cancelled-move");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (plugin.getTeleportService().isTeleporting(player.getUniqueId())) {
            plugin.getTeleportService().cancel(player, "warps.cancelled-damage");
        }
        if (plugin.getTeleportService().isProtected(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getTeleportService().cancelSilently(event.getPlayer().getUniqueId());
    }
}
