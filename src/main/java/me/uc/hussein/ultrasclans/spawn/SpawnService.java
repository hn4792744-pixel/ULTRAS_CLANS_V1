package me.uc.hussein.ultrasclans.spawn;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import me.uc.hussein.ultrasclans.model.ClanSpawn;
import me.uc.hussein.ultrasclans.repository.SpawnRepository;
import me.uc.hussein.ultrasclans.teleport.SafeLocationFinder;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class SpawnService {

    public enum SetFail { NO_PERMISSION }
    public enum UseFail { NO_PERMISSION, NOT_SET, WORLD_UNLOADED, UNSAFE }

    private final UltrasClansPlugin plugin;
    private final SpawnRepository repository;

    public SpawnService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.repository = new SpawnRepository(plugin.getDatabaseManager());
    }

    public void set(Player player, Clan clan, java.util.function.Consumer<SetFail> onFail, Runnable onSuccess) {
        boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_SPAWN_SET)
                || plugin.getPermissionService().isOwner(player.getUniqueId());
        if (!allowed) {
            onFail.accept(SetFail.NO_PERMISSION);
            return;
        }
        Location loc = player.getLocation();
        ClanSpawn spawn = new ClanSpawn(clan.getId(), loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        plugin.getScheduler().runAsync(() -> repository.upsert(spawn))
                .whenComplete((v, error) -> plugin.getScheduler().runSync(() -> {
                    if (error == null) onSuccess.run();
                }));
    }

    public void use(Player player, Clan clan, java.util.function.Consumer<UseFail> onFail) {
        boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_SPAWN_USE)
                || plugin.getPermissionService().isOwner(player.getUniqueId());
        if (!allowed) {
            onFail.accept(UseFail.NO_PERMISSION);
            return;
        }
        plugin.getScheduler().supplyAsync(() -> repository.find(clan.getId())).whenComplete((opt, error) ->
                plugin.getScheduler().runSync(() -> {
                    if (error != null || opt.isEmpty()) {
                        onFail.accept(UseFail.NOT_SET);
                        return;
                    }
                    ClanSpawn spawn = opt.get();
                    var world = plugin.getServer().getWorld(spawn.getWorld());
                    if (world == null) {
                        onFail.accept(UseFail.WORLD_UNLOADED);
                        return;
                    }
                    Location target = new Location(world, spawn.getX(), spawn.getY(), spawn.getZ(),
                            spawn.getYaw(), spawn.getPitch());
                    int radius = plugin.getConfigManager().loadYaml("warps.yml").getInt("safe-search-radius", 5);
                    var safe = SafeLocationFinder.find(target, radius);
                    if (safe.isEmpty()) {
                        onFail.accept(UseFail.UNSAFE);
                        return;
                    }
                    long delay = plugin.getConfigManager().loadYaml("warps.yml").getLong("spawn-delay-seconds", 3);
                    plugin.getTeleportService().start(player, safe.get(), delay, "warps");
                }));
    }

    public CompletableFuture<Optional<ClanSpawn>> get(java.util.UUID clanId) {
        return plugin.getScheduler().supplyAsync(() -> repository.find(clanId));
    }
}
