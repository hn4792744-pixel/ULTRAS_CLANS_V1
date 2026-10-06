package me.uc.hussein.ultrasclans.listener;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.event.ParticipantState;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class EventCombatListener implements Listener {

    private final UltrasClansPlugin plugin;

    public EventCombatListener(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (plugin.getEventService().isInActiveEvent(event.getEntity().getUniqueId())) {
            plugin.getEventService().onParticipantEliminated(event.getEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        var stateOpt = plugin.getEventService().getParticipantState(player.getUniqueId());
        if (stateOpt.isEmpty() || stateOpt.get() != ParticipantState.SPECTATOR) {
            return;
        }
        Location target = plugin.getEventService().getSpectatorRespawnLocation(player.getUniqueId());
        if (target != null) {
            event.setRespawnLocation(target);
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SPECTATOR));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.getEventService().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getEventService().isFrozen(player.getUniqueId())) return;
        if (event.getFrom().getX() == event.getTo().getX()
                && event.getFrom().getY() == event.getTo().getY()
                && event.getFrom().getZ() == event.getTo().getZ()) {
            return; // دوران الرأس فقط مسموح
        }
        event.setTo(event.getFrom());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getEventService().isInActiveEvent(player.getUniqueId())) {
            return;
        }
        String command = event.getMessage().toLowerCase();
        boolean allowed = command.startsWith("/clan event") || command.equals("/clan");
        if (!allowed) {
            event.setCancelled(true);
            plugin.getMessageService().send(player, "events.cannot-use-commands");
        }
    }
}
