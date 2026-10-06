package me.uc.hussein.ultrasclans.warp;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import me.uc.hussein.ultrasclans.model.ClanWarp;
import me.uc.hussein.ultrasclans.repository.WarpRepository;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class WarpService {

    public enum CreateFail { NO_PERMISSION, LIMIT_REACHED, MAX_ABSOLUTE_REACHED }
    public enum DeleteFail { NO_PERMISSION, NOT_FOUND }
    public enum UseFail { NO_PERMISSION, NOT_FOUND, WORLD_UNLOADED, UNSAFE }

    private final UltrasClansPlugin plugin;
    private final WarpRepository repository;

    public WarpService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.repository = new WarpRepository(plugin.getDatabaseManager());
    }

    public CompletableFuture<List<ClanWarp>> listWarps(UUID clanId) {
        return plugin.getScheduler().supplyAsync(() -> repository.findByClan(clanId));
    }

    public void create(Player player, Clan clan, java.util.function.Consumer<CreateFail> onFail,
                        java.util.function.Consumer<ClanWarp> onSuccess) {
        boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_WARP_CREATE)
                || plugin.getPermissionService().isOwner(player.getUniqueId());
        if (!allowed) {
            onFail.accept(CreateFail.NO_PERMISSION);
            return;
        }

        int absoluteMax = plugin.getConfigManager().loadYaml("warps.yml").getInt("max-warps", 5);
        Location loc = player.getLocation();

        plugin.getScheduler().supplyAsync(() -> repository.countByClan(clan.getId())).whenComplete((count, error) ->
                plugin.getScheduler().runSync(() -> {
                    if (error != null) {
                        return;
                    }
                    if (count >= absoluteMax) {
                        onFail.accept(CreateFail.MAX_ABSOLUTE_REACHED);
                        return;
                    }
                    if (count >= clan.getWarpCapacity()) {
                        onFail.accept(CreateFail.LIMIT_REACHED);
                        return;
                    }
                    int newId = count + 1;
                    ClanWarp warp = new ClanWarp(clan.getId(), newId, loc.getWorld().getName(),
                            loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(),
                            player.getUniqueId(), System.currentTimeMillis());

                    plugin.getScheduler().runAsync(() -> repository.insert(warp))
                            .whenComplete((v, insertError) -> plugin.getScheduler().runSync(() -> {
                                if (insertError == null) {
                                    onSuccess.accept(warp);
                                }
                            }));
                }));
    }

    public void delete(Player player, Clan clan, int warpId, java.util.function.Consumer<DeleteFail> onFail,
                        Runnable onSuccess) {
        boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_WARP_DELETE)
                || plugin.getPermissionService().isOwner(player.getUniqueId());
        if (!allowed) {
            onFail.accept(DeleteFail.NO_PERMISSION);
            return;
        }
        plugin.getScheduler().supplyAsync(() -> repository.find(clan.getId(), warpId)).whenComplete((opt, error) -> {
            if (error != null || opt.isEmpty()) {
                plugin.getScheduler().runSync(() -> onFail.accept(DeleteFail.NOT_FOUND));
                return;
            }
            plugin.getScheduler().runAsync(() -> repository.delete(clan.getId(), warpId))
                    .whenComplete((v, delError) -> plugin.getScheduler().runSync(onSuccess));
        });
    }

    public void teleport(Player player, Clan clan, int warpId, java.util.function.Consumer<UseFail> onFail) {
        boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_WARP_USE)
                || plugin.getPermissionService().isOwner(player.getUniqueId());
        if (!allowed) {
            onFail.accept(UseFail.NO_PERMISSION);
            return;
        }
        plugin.getScheduler().supplyAsync(() -> repository.find(clan.getId(), warpId)).whenComplete((opt, error) ->
                plugin.getScheduler().runSync(() -> {
                    if (error != null || opt.isEmpty()) {
                        onFail.accept(UseFail.NOT_FOUND);
                        return;
                    }
                    ClanWarp warp = opt.get();
                    var world = plugin.getServer().getWorld(warp.getWorld());
                    if (world == null) {
                        onFail.accept(UseFail.WORLD_UNLOADED);
                        return;
                    }
                    Location target = new Location(world, warp.getX(), warp.getY(), warp.getZ(),
                            warp.getYaw(), warp.getPitch());
                    long delay = plugin.getConfigManager().loadYaml("warps.yml").getLong("warp-delay-seconds", 5);
                    plugin.getTeleportService().start(player, target, delay, "warps");
                }));
    }
}
