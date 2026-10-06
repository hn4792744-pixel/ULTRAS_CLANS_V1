package me.uc.hussein.ultrasclans.listener;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * يمنع الـPvP بين أعضاء كلانين متحالفين إذا فعّل أي من الطرفين
 * "Prevent PvP" في إعدادات التحالف الخاصة به (قسم 27، مفعّل افتراضيًا).
 */
public final class AlliancePvpListener implements Listener {

    private final UltrasClansPlugin plugin;

    public AlliancePvpListener(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) {
            return;
        }
        var attackerClan = plugin.getClanService().getPlayerClan(attacker.getUniqueId());
        var victimClan = plugin.getClanService().getPlayerClan(victim.getUniqueId());
        if (attackerClan.isEmpty() || victimClan.isEmpty()) {
            return;
        }
        var alliance = plugin.getAllianceService().getAlliance(attackerClan.get().getId(), victimClan.get().getId());
        if (alliance.isEmpty()) {
            return;
        }
        boolean blocked = plugin.getAllianceService().getSettings(alliance.get().getId(), attackerClan.get().getId()).isPreventPvp()
                || plugin.getAllianceService().getSettings(alliance.get().getId(), victimClan.get().getId()).isPreventPvp();
        if (blocked) {
            event.setCancelled(true);
        }
    }
}
