package me.uc.hussein.ultrasclans.event;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.repository.AdminArenaRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ArenaService {

    private final UltrasClansPlugin plugin;
    private final AdminArenaRepository repository;
    private volatile AdminArena cached = new AdminArena();

    public ArenaService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.repository = new AdminArenaRepository(plugin.getDatabaseManager());
    }

    public CompletableFuture<Void> loadAll() {
        return plugin.getScheduler().supplyAsync(repository::load)
                .thenAccept(row -> plugin.getScheduler().runSync(() -> {
                    AdminArena arena = new AdminArena();
                    arena.setEvent1(toLocation(row.event1(), true));
                    arena.setEvent2(toLocation(row.event2(), true));
                    arena.setRest1(toLocation(row.rest1Corner(), false));
                    arena.setRest2(toLocation(row.rest2Corner(), false));
                    cached = arena;
                }));
    }

    private Location toLocation(AdminArenaRepository.Point point, boolean rotation) {
        if (point == null) return null;
        var world = Bukkit.getWorld(point.world());
        if (world == null) return null;
        return rotation
                ? new Location(world, point.x(), point.y(), point.z(), point.yaw(), point.pitch())
                : new Location(world, point.x(), point.y(), point.z());
    }

    public AdminArena get() {
        return cached;
    }

    public void setEvent1(Player admin) {
        cached.setEvent1(admin.getLocation());
        persist();
    }

    public void setEvent2(Player admin) {
        cached.setEvent2(admin.getLocation());
        persist();
    }

    public void setRest1(Player admin) {
        cached.setRest1(admin.getLocation());
        persist();
    }

    public void setRest2(Player admin) {
        cached.setRest2(admin.getLocation());
        persist();
    }

    private void persist() {
        AdminArena snapshot = cached;
        plugin.getScheduler().runAsync(() -> {
            if (snapshot.getEvent1() != null) repository.saveEvent1(toPoint(snapshot.getEvent1(), true));
            if (snapshot.getEvent2() != null) repository.saveEvent2(toPoint(snapshot.getEvent2(), true));
            if (snapshot.getRest1() != null) repository.saveRest1(toPoint(snapshot.getRest1(), false));
            if (snapshot.getRest2() != null) repository.saveRest2(toPoint(snapshot.getRest2(), false));
        });
    }

    private AdminArenaRepository.Point toPoint(Location loc, boolean rotation) {
        return new AdminArenaRepository.Point(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(),
                rotation ? loc.getYaw() : 0f, rotation ? loc.getPitch() : 0f);
    }

    /**
     * يأخذ لقطة من منطقة rest1-rest2 الحالية (قبل بدء الحدث). يُقصّ حجم
     * المنطقة تلقائيًا لحماية الأداء (events.yml: admin-arena.max-region-size).
     * يجب استدعاؤها على الـmain thread فقط (قراءة/كتابة كتل).
     */
    public ArenaSnapshot snapshot() {
        Location a = cached.getRest1();
        Location b = cached.getRest2();
        if (a == null || b == null || a.getWorld() == null) {
            return null;
        }
        int maxSize = plugin.getConfigManager().loadYaml("events.yml")
                .getInt("admin-arena.max-region-size", 64);

        int minX = Math.min(a.getBlockX(), b.getBlockX());
        int maxX = Math.min(minX + maxSize, Math.max(a.getBlockX(), b.getBlockX()));
        int minY = Math.min(a.getBlockY(), b.getBlockY());
        int maxY = Math.min(minY + maxSize, Math.max(a.getBlockY(), b.getBlockY()));
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        int maxZ = Math.min(minZ + maxSize, Math.max(a.getBlockZ(), b.getBlockZ()));

        var world = a.getWorld();
        var entries = new java.util.ArrayList<ArenaSnapshot.Entry>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    var block = world.getBlockAt(x, y, z);
                    entries.add(new ArenaSnapshot.Entry(x, y, z, block.getBlockData().clone()));
                }
            }
        }
        return new ArenaSnapshot(world.getName(), entries.toArray(new ArenaSnapshot.Entry[0]));
    }

    /** يستعيد الكتل من لقطة سابقة، ويحذف الأغراض الساقطة إن كان مفعّلًا. يجب استدعاؤها على الـmain thread. */
    public void restore(ArenaSnapshot snapshot) {
        if (snapshot == null) return;
        var world = Bukkit.getWorld(snapshot.getWorld());
        if (world == null) return;

        for (ArenaSnapshot.Entry entry : snapshot.getEntries()) {
            var block = world.getBlockAt(entry.x(), entry.y(), entry.z());
            block.setBlockData(entry.data(), false);
        }

        boolean clearItems = plugin.getConfigManager().loadYaml("events.yml")
                .getBoolean("admin-arena.clear-dropped-items-on-reset", true);
        if (!clearItems || snapshot.getEntries().length == 0) {
            return;
        }
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (ArenaSnapshot.Entry e : snapshot.getEntries()) {
            minX = Math.min(minX, e.x()); maxX = Math.max(maxX, e.x());
            minY = Math.min(minY, e.y()); maxY = Math.max(maxY, e.y());
            minZ = Math.min(minZ, e.z()); maxZ = Math.max(maxZ, e.z());
        }
        double pad = 2.0;
        var minLoc = new Location(world, minX - pad, minY - pad, minZ - pad);
        var maxLoc = new Location(world, maxX + pad, maxY + pad, maxZ + pad);
        var box = org.bukkit.util.BoundingBox.of(minLoc, maxLoc);
        List<Item> items = world.getEntitiesByClass(Item.class).stream()
                .filter(i -> box.contains(i.getLocation().toVector())).toList();
        items.forEach(Item::remove);
    }
}
