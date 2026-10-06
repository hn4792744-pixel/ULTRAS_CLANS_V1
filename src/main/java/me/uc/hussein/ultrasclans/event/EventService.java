package me.uc.hussein.ultrasclans.event;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import me.uc.hussein.ultrasclans.repository.EventParticipantRepository;
import me.uc.hussein.ultrasclans.repository.EventRepository;
import me.uc.hussein.ultrasclans.teleport.SafeLocationFinder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * الخدمة المركزية لحروب الكلانات (Clan Events/Wars). تجمع بين حالة
 * دائمة في قاعدة البيانات (ClanEvent, EventParticipant) وحالة لحظية في
 * الذاكرة فقط (مواعيد نهاية الجولة، لقطة الساحة) لا تحتاج النجاة من
 * إعادة التشغيل - راجع README لتوثيق هذا القرار.
 */
public final class EventService {

    public enum CreateFail {
        SELF, ALREADY_IN_EVENT, ALREADY_PENDING, NO_WAGER, NO_PERMISSION, NO_LOCATION, TARGET_DISABLED
    }

    public enum RespondFail {
        NONE_PENDING, NOT_ENOUGH_MONEY, NOT_ENOUGH_CP, NO_ONLINE_MEMBERS, NO_LOCATION
    }

    /** حالة لحظية لحدث نشط واحد - لا تُحفظ في قاعدة البيانات. */
    private static final class RuntimeState {
        final Map<UUID, EventParticipant> participants = new ConcurrentHashMap<>();
        volatile long roundDeadline;
        volatile boolean frozen;
        ArenaSnapshot arenaSnapshot;
    }

    private final UltrasClansPlugin plugin;
    private final EventRepository repository;
    private final EventParticipantRepository participantRepository;

    private final Map<UUID, ClanEvent> events = new ConcurrentHashMap<>();
    private final Map<UUID, RuntimeState> runtime = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> clanActiveEvent = new ConcurrentHashMap<>();
    /** playerUuid -> eventId، للوصول السريع من المستمعين (موت/قطع اتصال/أوامر). */
    private final Map<UUID, UUID> playerEvent = new ConcurrentHashMap<>();

    public EventService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.repository = new EventRepository(plugin.getDatabaseManager());
        this.participantRepository = new EventParticipantRepository(plugin.getDatabaseManager());
    }

    /**
     * عند إقلاع البلاجن: أي حدث كان نشطًا أو قيد التحضير وقت إيقاف
     * السيرفر السابق لا يمكن استئنافه بأمان (حالة اللاعبين داخل المعركة
     * غير معروفة)، فيُلغى تلقائيًا ويُرَدّ الرهان لكلا الطرفين (قسم 84:
     * لا فقدان بيانات، لكن أيضًا لا نخاطر باستئناف قتال غير متّسق).
     */
    public void loadAll() {
        plugin.getScheduler().supplyAsync(repository::findAllActiveOrPreparing)
                .whenComplete((list, error) -> plugin.getScheduler().runSync(() -> {
                    if (error != null || list == null) return;
                    for (ClanEvent event : list) {
                        plugin.getLogger().warning("Event " + event.getId() + " was active during the last shutdown - "
                                + "cancelling and refunding both clans for safety.");
                        refundWager(event);
                        event.setStatus(EventStatus.CANCELLED);
                        event.setEndedAt(System.currentTimeMillis());
                        plugin.getScheduler().runAsync(() -> repository.update(event));
                    }
                }));
    }

    /**
     * عند إيقاف البلاجن أثناء أحداث نشطة: نُعيد كل مشارك إلى موقعه السابق وSurvival،
     * ونستعيد الساحة الإدارية. الرهان يُرَدّ تلقائيًا عند الإقلاع القادم (loadAll) لأن
     * حالة الحدث تبقى ACTIVE في قاعدة البيانات.
     */
    public void shutdown() {
        for (var entry : runtime.entrySet()) {
            RuntimeState state = entry.getValue();
            for (EventParticipant p : state.participants.values()) {
                Player player = plugin.getServer().getPlayer(p.getPlayerUuid());
                if (player == null) continue;
                player.setGameMode(GameMode.SURVIVAL);
                var world = plugin.getServer().getWorld(p.getPreWorld());
                if (world != null) {
                    player.teleport(new Location(world, p.getPreX(), p.getPreY(), p.getPreZ(), p.getPreYaw(), p.getPrePitch()));
                }
            }
            if (state.arenaSnapshot != null) {
                plugin.getArenaService().restore(state.arenaSnapshot);
            }
        }
        runtime.clear();
    }

    // ------------------------------------------------------------ إنشاء التحدي

    public void createChallenge(Player sender, Clan senderClan, Clan targetClan, LocationType locationType,
                                 int roundDurationSeconds, int totalRounds, double moneyWager, long cpWager,
                                 Consumer<CreateFail> onFail, Runnable onSuccess) {
        if (senderClan.getId().equals(targetClan.getId())) {
            onFail.accept(CreateFail.SELF);
            return;
        }
        boolean allowed = plugin.getPermissionService().hasPermission(sender.getUniqueId(), ClanPermission.CLAN_EVENT_CREATE)
                || plugin.getPermissionService().isOwner(sender.getUniqueId());
        if (!allowed) {
            onFail.accept(CreateFail.NO_PERMISSION);
            return;
        }
        if (clanActiveEvent.containsKey(senderClan.getId()) || clanActiveEvent.containsKey(targetClan.getId())) {
            onFail.accept(CreateFail.ALREADY_IN_EVENT);
            return;
        }
        if (moneyWager <= 0 && cpWager <= 0) {
            onFail.accept(CreateFail.NO_WAGER);
            return;
        }
        if (!plugin.getPrefsService().anyManagerAccepts(targetClan, ClanPermission.CLAN_EVENT_ACCEPT,
                me.uc.hussein.ultrasclans.model.PlayerPrefs.Setting.CHALLENGES)) {
            onFail.accept(CreateFail.TARGET_DISABLED);
            return;
        }
        if (locationType == LocationType.ADMIN_ARENA && !plugin.getArenaService().get().isFullyConfigured()) {
            onFail.accept(CreateFail.NO_LOCATION);
            return;
        }

        plugin.getScheduler().supplyAsync(() -> repository.findPendingBetween(senderClan.getId(), targetClan.getId()))
                .whenComplete((existing, error) -> plugin.getScheduler().runSync(() -> {
                    if (error == null && existing.isPresent()) {
                        onFail.accept(CreateFail.ALREADY_PENDING);
                        return;
                    }
                    long expiryMinutes = plugin.getConfigManager().loadYaml("events.yml")
                            .getLong("challenge-expiry-minutes", 30);
                    ClanEvent event = new ClanEvent(UUID.randomUUID(), senderClan.getId(), targetClan.getId(),
                            EventStatus.PENDING_INVITE, locationType, roundDurationSeconds, totalRounds,
                            0, 0, 0, moneyWager, cpWager, null, sender.getUniqueId(),
                            System.currentTimeMillis(), null, null);

                    plugin.getScheduler().runAsync(() -> repository.insert(event))
                            .whenComplete((v, insertError) -> plugin.getScheduler().runSync(() -> {
                                if (insertError != null) return;
                                events.put(event.getId(), event);
                                onSuccess.run();
                            }));
                }));
    }

    public Optional<ClanEvent> findPendingChallenge(UUID clanA, UUID clanB) {
        return events.values().stream()
                .filter(e -> e.getStatus() == EventStatus.PENDING_INVITE)
                .filter(e -> (e.getClanA().equals(clanA) && e.getClanB().equals(clanB))
                        || (e.getClanA().equals(clanB) && e.getClanB().equals(clanA)))
                .findFirst();
    }

    public void denyChallenge(ClanEvent event) {
        event.setStatus(EventStatus.CANCELLED);
        events.remove(event.getId());
        plugin.getScheduler().runAsync(() -> repository.update(event));
    }

    // ------------------------------------------------------------ القبول والبدء

    public void accept(ClanEvent event, java.util.function.Consumer<RespondFail> onFail) {
        Optional<Clan> clanAOpt = plugin.getClanService().getClan(event.getClanA());
        Optional<Clan> clanBOpt = plugin.getClanService().getClan(event.getClanB());
        if (clanAOpt.isEmpty() || clanBOpt.isEmpty()) {
            onFail.accept(RespondFail.NONE_PENDING);
            return;
        }
        Clan clanA = clanAOpt.get();
        Clan clanB = clanBOpt.get();

        // إذا كان نوع الموقع Clan Spawn، يتحقق resolveStartLocations لاحقًا من وجود
        // سبون فعلي لكلا الكلانين ويُفشل بـNO_LOCATION إن كان أي منهما غير محدَّد.

        List<Player> onlineA = onlineMembers(clanA.getId());
        List<Player> onlineB = onlineMembers(clanB.getId());
        if (onlineA.isEmpty() || onlineB.isEmpty()) {
            onFail.accept(RespondFail.NO_ONLINE_MEMBERS);
            return;
        }

        for (Clan clan : List.of(clanA, clanB)) {
            RespondFail shortage = affordFailure(clan, event.getMoneyWager(), event.getCpWager());
            if (shortage != null) {
                onFail.accept(shortage);
                return;
            }
        }

        resolveStartLocations(event, clanA, clanB, (locA, locB) -> {
            if (locA == null || locB == null) {
                onFail.accept(RespondFail.NO_LOCATION);
                return;
            }

            deductWager(clanA, event.getMoneyWager(), event.getCpWager());
            deductWager(clanB, event.getMoneyWager(), event.getCpWager());

            RuntimeState state = new RuntimeState();
            List<EventParticipant> toInsert = new ArrayList<>();
            for (Player p : onlineA) {
                EventParticipant participant = buildParticipant(event.getId(), p, clanA.getId());
                state.participants.put(p.getUniqueId(), participant);
                toInsert.add(participant);
                playerEvent.put(p.getUniqueId(), event.getId());
            }
            for (Player p : onlineB) {
                EventParticipant participant = buildParticipant(event.getId(), p, clanB.getId());
                state.participants.put(p.getUniqueId(), participant);
                toInsert.add(participant);
                playerEvent.put(p.getUniqueId(), event.getId());
            }
            runtime.put(event.getId(), state);

            event.setStatus(EventStatus.ACCEPTED_PREPARING);
            event.setStartedAt(System.currentTimeMillis());
            clanActiveEvent.put(clanA.getId(), event.getId());
            clanActiveEvent.put(clanB.getId(), event.getId());
            events.put(event.getId(), event);

            plugin.getScheduler().runAsync(() -> {
                repository.update(event);
                participantRepository.insertAll(toInsert);
            });

            notifyClanSimple(clanA.getId(), "events.accepted-notify", clanB);
            notifyClanSimple(clanB.getId(), "events.accepted-notify", clanA);
            for (Player p : onlineA) plugin.getMessageService().send(p, "events.accepted");
            for (Player p : onlineB) plugin.getMessageService().send(p, "events.accepted");

            startPreparation(event, locA, locB);
        });
    }

    private void notifyClanSimple(UUID clanId, String key, Clan otherClan) {
        for (var member : plugin.getClanService().getMembers(clanId)) {
            Player online = plugin.getServer().getPlayer(member.getUuid());
            if (online != null) {
                plugin.getMessageService().send(online, key, Map.of("clan", otherClan.getName()));
            }
        }
    }

    /** نقطة موحّدة لقبول تحدٍّ من الأوامر والـGUI، مع فحص الصلاحية ورسائل الأخطاء. */
    public void handleAccept(Player player, ClanEvent event) {
        var clanOpt = plugin.getClanService().getPlayerClan(player.getUniqueId());
        if (clanOpt.isEmpty() || !event.getClanB().equals(clanOpt.get().getId())
                || event.getStatus() != EventStatus.PENDING_INVITE) {
            plugin.getMessageService().send(player, "events.none-pending");
            return;
        }
        boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_EVENT_ACCEPT)
                || plugin.getPermissionService().isOwner(player.getUniqueId());
        if (!allowed) {
            plugin.getMessageService().send(player, "events.no-permission");
            return;
        }
        accept(event, fail -> {
            String key = switch (fail) {
                case NONE_PENDING -> "events.none-pending";
                case NOT_ENOUGH_MONEY -> "events.not-enough-money";
                case NOT_ENOUGH_CP -> "events.not-enough-cp";
                case NO_ONLINE_MEMBERS -> "events.no-online-members";
                case NO_LOCATION -> "events.no-location";
            };
            plugin.getMessageService().send(player, key);
        });
    }

    public void handleDeny(Player player, ClanEvent event) {
        var clanOpt = plugin.getClanService().getPlayerClan(player.getUniqueId());
        if (clanOpt.isEmpty() || !event.getClanB().equals(clanOpt.get().getId())
                || event.getStatus() != EventStatus.PENDING_INVITE) {
            plugin.getMessageService().send(player, "events.none-pending");
            return;
        }
        boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_EVENT_ACCEPT)
                || plugin.getPermissionService().isOwner(player.getUniqueId());
        if (!allowed) {
            plugin.getMessageService().send(player, "events.no-permission");
            return;
        }
        denyChallenge(event);
        plugin.getMessageService().send(player, "events.denied");
        plugin.getClanService().getClan(event.getClanA()).ifPresent(challenger ->
                plugin.getClanService().getClan(event.getClanB()).ifPresent(decliner ->
                        notifyClanSimple(challenger.getId(), "events.denied-notify", decliner)));
    }

    private EventParticipant buildParticipant(UUID eventId, Player player, UUID clanId) {
        Location loc = player.getLocation();
        return new EventParticipant(eventId, player.getUniqueId(), clanId, ParticipantState.ALIVE,
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), null);
    }

    private List<Player> onlineMembers(UUID clanId) {
        List<Player> result = new ArrayList<>();
        for (var member : plugin.getClanService().getMembers(clanId)) {
            Player p = plugin.getServer().getPlayer(member.getUuid());
            if (p != null) result.add(p);
        }
        return result;
    }

    /** @return null إذا كان الكلان قادرًا على دفع الرهان، وإلا سبب العجز المحدد. */
    private RespondFail affordFailure(Clan clan, double money, long cp) {
        synchronized (clan) {
            if (clan.getBankBalance() < money) return RespondFail.NOT_ENOUGH_MONEY;
            if (clan.getCp() < cp) return RespondFail.NOT_ENOUGH_CP;
            return null;
        }
    }

    private void deductWager(Clan clan, double money, long cp) {
        synchronized (clan) {
            clan.setBankBalance(clan.getBankBalance() - money);
            clan.setCp(clan.getCp() - cp);
        }
        plugin.getScheduler().runAsync(() -> plugin.getClanService().getClanRepository().update(clan));
    }

    private void payWager(Clan clan, double money, long cp) {
        synchronized (clan) {
            clan.setBankBalance(clan.getBankBalance() + money);
        }
        plugin.getScheduler().runAsync(() -> plugin.getClanService().getClanRepository().update(clan));
        if (cp > 0) {
            plugin.getPointsService().addCpForClan(clan, cp, "Event reward");
        }
    }

    private void refundWager(ClanEvent event) {
        plugin.getClanService().getClan(event.getClanA())
                .ifPresent(c -> payWager(c, event.getMoneyWager(), event.getCpWager()));
        plugin.getClanService().getClan(event.getClanB())
                .ifPresent(c -> payWager(c, event.getMoneyWager(), event.getCpWager()));
    }

    /** يحدد موقعي بدء كل فريق حسب نوع الموقع المختار (قسم 34). */
    private void resolveStartLocations(ClanEvent event, Clan clanA, Clan clanB,
                                        java.util.function.BiConsumer<Location, Location> callback) {
        switch (event.getLocationType()) {
            case ADMIN_ARENA -> {
                AdminArena arena = plugin.getArenaService().get();
                callback.accept(arena.getEvent1(), arena.getEvent2());
            }
            case CLAN_SPAWN -> plugin.getSpawnService().get(clanA.getId()).whenComplete((spawnAOpt, e1) ->
                    plugin.getScheduler().runSync(() -> plugin.getSpawnService().get(clanB.getId()).whenComplete((spawnBOpt, e2) ->
                            plugin.getScheduler().runSync(() -> {
                                Location locA = toLocation(spawnAOpt);
                                Location locB = toLocation(spawnBOpt);
                                callback.accept(locA, locB);
                            }))));
            case RANDOM_SAFE -> {
                var config = plugin.getConfigManager().loadYaml("events.yml");
                int radius = config.getInt("random-location.search-radius", 2000);
                int attempts = config.getInt("random-location.max-attempts", 20);
                var world = plugin.getServer().getWorlds().get(0);
                Location base = world.getSpawnLocation();
                Location found = null;
                for (int i = 0; i < attempts && found == null; i++) {
                    int dx = (int) ((Math.random() - 0.5) * 2 * radius);
                    int dz = (int) ((Math.random() - 0.5) * 2 * radius);
                    Location candidate = base.clone().add(dx, 0, dz);
                    candidate.setY(world.getHighestBlockYAt(candidate) + 1);
                    var safe = SafeLocationFinder.find(candidate, 10);
                    if (safe.isPresent()) found = safe.get();
                }
                Location locB = null;
                if (found != null) {
                    Location offsetGuess = found.clone().add(15, 0, 15);
                    offsetGuess.setY(world.getHighestBlockYAt(offsetGuess) + 1);
                    locB = SafeLocationFinder.find(offsetGuess, 10).orElse(null);
                }
                callback.accept(found, locB);
            }
        }
    }

    private Location toLocation(Optional<me.uc.hussein.ultrasclans.model.ClanSpawn> opt) {
        if (opt.isEmpty()) return null;
        var spawn = opt.get();
        var world = plugin.getServer().getWorld(spawn.getWorld());
        if (world == null) return null;
        return new Location(world, spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch());
    }

    // ------------------------------------------------------------ التحضير والعدّ التنازلي

    private void startPreparation(ClanEvent event, Location locA, Location locB) {
        RuntimeState state = runtime.get(event.getId());
        if (state == null) return;
        state.frozen = true;

        if (event.getLocationType() == LocationType.ADMIN_ARENA) {
            state.arenaSnapshot = plugin.getArenaService().snapshot();
        }

        int i = 0;
        for (EventParticipant p : state.participants.values()) {
            Player player = plugin.getServer().getPlayer(p.getPlayerUuid());
            if (player == null) continue;
            Location base = p.getClanId().equals(event.getClanA()) ? locA : locB;
            Location spread = base.clone().add((i % 5) - 2, 0, (i / 5) - 2);
            player.teleport(spread);
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
            player.setFoodLevel(20);
            i++;
        }

        long prepSeconds = plugin.getConfigManager().loadYaml("events.yml").getLong("preparation-seconds", 15);
        broadcastActionBar(state, "events.preparation", Map.of("seconds", String.valueOf(prepSeconds)));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> finalCountdown(event), prepSeconds * 20L);
    }

    private void finalCountdown(ClanEvent event) {
        RuntimeState state = runtime.get(event.getId());
        if (state == null) return;
        int[] n = {5};
        var task = new Object() {
            org.bukkit.scheduler.BukkitTask handle;
        };
        task.handle = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (n[0] > 0) {
                broadcastTitle(state, "events.countdown", Map.of("number", String.valueOf(n[0])));
                n[0]--;
            } else {
                broadcastTitle(state, "events.fight", Map.of());
                state.frozen = false;
                task.handle.cancel();
                startRound(event, 1);
            }
        }, 0L, 20L);
    }

    // ------------------------------------------------------------ الجولات

    private void startRound(ClanEvent event, int roundNumber) {
        RuntimeState state = runtime.get(event.getId());
        if (state == null) return;

        event.setCurrentRound(roundNumber);
        event.setStatus(EventStatus.ACTIVE);
        events.put(event.getId(), event);
        plugin.getScheduler().runAsync(() -> repository.update(event));

        for (EventParticipant p : state.participants.values()) {
            if (p.getState() == ParticipantState.LEFT) continue;
            p.setState(ParticipantState.ALIVE);
            Player player = plugin.getServer().getPlayer(p.getPlayerUuid());
            if (player != null) {
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
            }
        }

        state.roundDeadline = System.currentTimeMillis() + event.getRoundDurationSeconds() * 1000L;
        broadcastMessage(state, "events.round-start", Map.of(
                "round", String.valueOf(roundNumber), "total", String.valueOf(event.getTotalRounds())));
    }

    /** يُستدعى من المستمع عند موت/إقصاء مشارك. */
    public void onParticipantEliminated(UUID playerUuid) {
        UUID eventId = playerEvent.get(playerUuid);
        if (eventId == null) return;
        RuntimeState state = runtime.get(eventId);
        ClanEvent event = events.get(eventId);
        if (state == null || event == null || event.getStatus() != EventStatus.ACTIVE) return;

        EventParticipant p = state.participants.get(playerUuid);
        if (p == null || p.getState() != ParticipantState.ALIVE) return;
        p.setState(ParticipantState.SPECTATOR);

        Player player = plugin.getServer().getPlayer(playerUuid);
        if (player != null) {
            plugin.getMessageService().send(player, "events.eliminated");
            // تبديل الـgamemode الفعلي يحصل في PlayerRespawnEvent (راجع EventCombatListener)
            // - نقطة ربط أكثر موثوقية من تعديله فورًا أثناء شاشة الموت.
        }
        checkRoundEnd(event, state);
    }

    private boolean isSideAlive(RuntimeState state, UUID clanId) {
        return state.participants.values().stream()
                .anyMatch(p -> p.getClanId().equals(clanId) && p.getState() == ParticipantState.ALIVE);
    }

    private void checkRoundEnd(ClanEvent event, RuntimeState state) {
        boolean aliveA = isSideAlive(state, event.getClanA());
        boolean aliveB = isSideAlive(state, event.getClanB());
        if (aliveA && aliveB) return; // الجولة مستمرة

        if (!aliveA && !aliveB) {
            endRound(event, state, null); // تعادل نادر (إقصاء متزامن)
        } else if (!aliveA) {
            endRound(event, state, event.getClanB());
        } else {
            endRound(event, state, event.getClanA());
        }
    }

    /** يُستدعى من EventTickTask عند انتهاء وقت الجولة دون حسم. */
    public void onRoundTimeout(UUID eventId) {
        ClanEvent event = events.get(eventId);
        RuntimeState state = runtime.get(eventId);
        if (event == null || state == null || event.getStatus() != EventStatus.ACTIVE) return;
        broadcastMessage(state, "events.round-draw", Map.of("round", String.valueOf(event.getCurrentRound())));
        endRound(event, state, null);
    }

    private void endRound(ClanEvent event, RuntimeState state, UUID winningClanId) {
        state.roundDeadline = 0;
        if (winningClanId != null) {
            synchronized (event) {
                if (winningClanId.equals(event.getClanA())) {
                    event.setScoreA(event.getScoreA() + 1);
                } else {
                    event.setScoreB(event.getScoreB() + 1);
                }
            }
            for (EventParticipant p : state.participants.values()) {
                Player player = plugin.getServer().getPlayer(p.getPlayerUuid());
                if (player == null) continue;
                boolean won = p.getClanId().equals(winningClanId);
                plugin.getMessageService().send(player, won ? "events.round-win" : "events.round-lose",
                        Map.of("round", String.valueOf(event.getCurrentRound()),
                                "scoreA", String.valueOf(event.getScoreA()), "scoreB", String.valueOf(event.getScoreB())));
            }
        }

        int majority = (event.getTotalRounds() / 2) + 1;
        boolean decided = event.getScoreA() >= majority || event.getScoreB() >= majority
                || event.getCurrentRound() >= event.getTotalRounds();
        if (decided) {
            endEvent(event, EndReason.NORMAL);
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> startRound(event, event.getCurrentRound() + 1), 100L); // 5s فاصل بين الجولات
    }

    // ------------------------------------------------------------ النهاية

    private enum EndReason { NORMAL, FORFEIT_A, FORFEIT_B, MUTUAL_ABANDON }

    private void endEvent(ClanEvent event, EndReason reason) {
        RuntimeState state = runtime.remove(event.getId());
        events.remove(event.getId());
        clanActiveEvent.remove(event.getClanA());
        clanActiveEvent.remove(event.getClanB());

        UUID winner = switch (reason) {
            case NORMAL -> event.getScoreA() > event.getScoreB() ? event.getClanA()
                    : (event.getScoreB() > event.getScoreA() ? event.getClanB() : null);
            case FORFEIT_A -> event.getClanB();
            case FORFEIT_B -> event.getClanA();
            case MUTUAL_ABANDON -> null;
        };
        event.setWinnerClanId(winner);
        event.setStatus(EventStatus.FINISHED);
        event.setEndedAt(System.currentTimeMillis());

        if (state != null) {
            for (EventParticipant p : state.participants.values()) {
                Player player = plugin.getServer().getPlayer(p.getPlayerUuid());
                if (player == null) continue;
                player.setGameMode(GameMode.SURVIVAL);
                var world = plugin.getServer().getWorld(p.getPreWorld());
                if (world != null) {
                    player.teleport(new Location(world, p.getPreX(), p.getPreY(), p.getPreZ(), p.getPreYaw(), p.getPrePitch()));
                }
                playerEvent.remove(p.getPlayerUuid());
            }
            if (state.arenaSnapshot != null) {
                plugin.getArenaService().restore(state.arenaSnapshot);
            }
        }

        distributeResult(event, reason, winner);

        plugin.getScheduler().runAsync(() -> {
            repository.update(event);
            participantRepository.deleteByEvent(event.getId());
        });
    }

    private void distributeResult(ClanEvent event, EndReason reason, UUID winnerClanId) {
        Optional<Clan> clanAOpt = plugin.getClanService().getClan(event.getClanA());
        Optional<Clan> clanBOpt = plugin.getClanService().getClan(event.getClanB());
        if (clanAOpt.isEmpty() || clanBOpt.isEmpty()) return;
        Clan clanA = clanAOpt.get();
        Clan clanB = clanBOpt.get();

        double pot = event.getMoneyWager() * 2;
        long cpPot = event.getCpWager() * 2;
        String resultKeyA;
        String resultKeyB;

        if (winnerClanId == null) {
            // تعادل أو انسحاب متبادل: يُرَدّ لكل طرف رهانه الخاص فقط
            payWager(clanA, event.getMoneyWager(), event.getCpWager());
            payWager(clanB, event.getMoneyWager(), event.getCpWager());
            resultKeyA = resultKeyB = reason == EndReason.MUTUAL_ABANDON ? "events.mutual-abandon" : "events.event-draw";
        } else {
            Clan winner = winnerClanId.equals(clanA.getId()) ? clanA : clanB;
            payWager(winner, pot, cpPot);
            boolean forfeit = reason == EndReason.FORFEIT_A || reason == EndReason.FORFEIT_B;
            resultKeyA = winnerClanId.equals(clanA.getId())
                    ? (forfeit ? "events.forfeit-win" : "events.event-win")
                    : (forfeit ? "events.forfeit-lose" : "events.event-lose");
            resultKeyB = winnerClanId.equals(clanB.getId())
                    ? (forfeit ? "events.forfeit-win" : "events.event-win")
                    : (forfeit ? "events.forfeit-lose" : "events.event-lose");
        }

        notifyClanResult(clanA, resultKeyA, event);
        notifyClanResult(clanB, resultKeyB, event);
    }

    private void notifyClanResult(Clan clan, String messageKey, ClanEvent event) {
        for (var member : plugin.getClanService().getMembers(clan.getId())) {
            Player online = plugin.getServer().getPlayer(member.getUuid());
            if (online != null) {
                plugin.getMessageService().send(online, messageKey, Map.of(
                        "scoreA", String.valueOf(event.getScoreA()), "scoreB", String.valueOf(event.getScoreB())));
            }
        }
    }

    // ------------------------------------------------------------ المغادرة والانسحاب

    /** /clan event leave confirm. */
    public void leaveEvent(Player player) {
        UUID eventId = playerEvent.get(player.getUniqueId());
        if (eventId == null) return;
        ClanEvent event = events.get(eventId);
        RuntimeState state = runtime.get(eventId);
        if (event == null || state == null) return;

        EventParticipant p = state.participants.get(player.getUniqueId());
        if (p == null || p.getState() == ParticipantState.LEFT) return;
        boolean wasAlive = p.getState() == ParticipantState.ALIVE;
        p.setState(ParticipantState.LEFT);
        playerEvent.remove(player.getUniqueId());

        var pre = p;
        var world = plugin.getServer().getWorld(pre.getPreWorld());
        player.setGameMode(GameMode.SURVIVAL);
        if (world != null) {
            player.teleport(new Location(world, pre.getPreX(), pre.getPreY(), pre.getPreZ(), pre.getPreYaw(), pre.getPrePitch()));
        }
        plugin.getMessageService().send(player, "events.left-event");

        if (wasAlive && event.getStatus() == EventStatus.ACTIVE) {
            checkRoundEnd(event, state);
        }
        checkForfeit(event, state);
    }

    private void checkForfeit(ClanEvent event, RuntimeState state) {
        boolean aRemaining = state.participants.values().stream()
                .anyMatch(p -> p.getClanId().equals(event.getClanA()) && p.getState() != ParticipantState.LEFT);
        boolean bRemaining = state.participants.values().stream()
                .anyMatch(p -> p.getClanId().equals(event.getClanB()) && p.getState() != ParticipantState.LEFT);
        if (aRemaining && bRemaining) return;
        if (!aRemaining && !bRemaining) {
            endEvent(event, EndReason.MUTUAL_ABANDON);
        } else if (!aRemaining) {
            endEvent(event, EndReason.FORFEIT_A);
        } else {
            endEvent(event, EndReason.FORFEIT_B);
        }
    }

    /** PlayerQuitEvent أثناء حدث نشط. */
    public void onPlayerDisconnect(UUID playerUuid) {
        UUID eventId = playerEvent.get(playerUuid);
        if (eventId == null) return;
        RuntimeState state = runtime.get(eventId);
        ClanEvent event = events.get(eventId);
        if (state == null || event == null) return;

        EventParticipant p = state.participants.get(playerUuid);
        if (p == null || p.getState() == ParticipantState.LEFT) return;
        boolean wasAlive = p.getState() == ParticipantState.ALIVE;
        p.setState(ParticipantState.DISCONNECTED);
        p.setDisconnectedAt(System.currentTimeMillis());

        long graceSeconds = plugin.getConfigManager().loadYaml("events.yml").getLong("disconnect-grace-seconds", 300);
        broadcastMessage(state, "events.disconnect-warning", Map.of(
                "player", plugin.getServer().getOfflinePlayer(playerUuid).getName(),
                "time", me.uc.hussein.ultrasclans.util.TimeFormatter.format(graceSeconds)));

        if (wasAlive && event.getStatus() == EventStatus.ACTIVE) {
            checkRoundEnd(event, state);
        }
    }

    /** PlayerJoinEvent - يُعيد اللاعب للحدث إن كان ضمن مهلة السماح. */
    public void onPlayerReconnect(Player player) {
        UUID eventId = playerEvent.get(player.getUniqueId());
        if (eventId == null) return;
        RuntimeState state = runtime.get(eventId);
        ClanEvent event = events.get(eventId);
        if (state == null || event == null) return;

        EventParticipant p = state.participants.get(player.getUniqueId());
        if (p == null || p.getState() != ParticipantState.DISCONNECTED) return;

        p.setState(event.getStatus() == EventStatus.ACTIVE ? ParticipantState.ALIVE : ParticipantState.SPECTATOR);
        p.setDisconnectedAt(null);
        plugin.getMessageService().send(player, "events.reconnected");

        Location loc = event.getClanA().equals(p.getClanId()) ? currentSideLocation(state, event.getClanA())
                : currentSideLocation(state, event.getClanB());
        if (loc != null) {
            player.teleport(loc);
        }
        player.setGameMode(p.getState() == ParticipantState.ALIVE ? GameMode.SURVIVAL : GameMode.SPECTATOR);
    }

    private Location currentSideLocation(RuntimeState state, UUID clanId) {
        return state.participants.values().stream()
                .filter(p -> p.getClanId().equals(clanId) && p.getState() == ParticipantState.ALIVE)
                .map(p -> plugin.getServer().getPlayer(p.getPlayerUuid()))
                .filter(java.util.Objects::nonNull)
                .map(Player::getLocation)
                .findFirst().orElse(null);
    }

    // ------------------------------------------------------------ يُستدعى من EventTickTask كل 5 ثوانٍ

    public void tick() {
        long now = System.currentTimeMillis();
        for (ClanEvent event : List.copyOf(events.values())) {
            if (event.getStatus() != EventStatus.ACTIVE) continue;
            RuntimeState state = runtime.get(event.getId());
            if (state == null) continue;

            if (state.roundDeadline > 0 && now >= state.roundDeadline) {
                onRoundTimeout(event.getId());
                continue;
            }

            long graceMs = plugin.getConfigManager().loadYaml("events.yml").getLong("disconnect-grace-seconds", 300) * 1000L;
            for (EventParticipant p : List.copyOf(state.participants.values())) {
                if (p.getState() == ParticipantState.DISCONNECTED && p.getDisconnectedAt() != null
                        && now - p.getDisconnectedAt() > graceMs) {
                    p.setState(ParticipantState.LEFT);
                    playerEvent.remove(p.getPlayerUuid()); // لا يبقى مقيَّدًا بحظر الأوامر عند عودته لاحقًا
                    checkForfeit(event, state);
                }
            }
        }
    }

    // ------------------------------------------------------------ أوامر مساعدة (حظر الأوامر أثناء الحدث)

    public boolean isInActiveEvent(UUID playerUuid) {
        return playerEvent.containsKey(playerUuid);
    }

    /** true أثناء فترة التحضير/العدّ التنازلي (قسم 37: لا حركة ولا ضرر ولا هجوم). */
    public boolean isFrozen(UUID playerUuid) {
        UUID eventId = playerEvent.get(playerUuid);
        if (eventId == null) return false;
        RuntimeState state = runtime.get(eventId);
        return state != null && state.frozen;
    }

    public Optional<ParticipantState> getParticipantState(UUID playerUuid) {
        UUID eventId = playerEvent.get(playerUuid);
        if (eventId == null) return Optional.empty();
        RuntimeState state = runtime.get(eventId);
        if (state == null) return Optional.empty();
        EventParticipant p = state.participants.get(playerUuid);
        return p == null ? Optional.empty() : Optional.of(p.getState());
    }

    /** يُستدعى من PlayerRespawnEvent لمعرفة أين يجب أن يظهر المشارك المُقصى (أحد زملائه الأحياء). */
    public Location getSpectatorRespawnLocation(UUID playerUuid) {
        UUID eventId = playerEvent.get(playerUuid);
        if (eventId == null) return null;
        RuntimeState state = runtime.get(eventId);
        if (state == null) return null;
        EventParticipant p = state.participants.get(playerUuid);
        if (p == null) return null;
        Location alive = currentSideLocation(state, p.getClanId());
        if (alive != null) return alive;
        var world = plugin.getServer().getWorld(p.getPreWorld());
        return world != null ? new Location(world, p.getPreX(), p.getPreY(), p.getPreZ()) : null;
    }

    public Optional<ClanEvent> getEventFor(UUID playerUuid) {
        UUID eventId = playerEvent.get(playerUuid);
        return eventId == null ? Optional.empty() : Optional.ofNullable(events.get(eventId));
    }

    public List<ClanEvent> getActiveOrPendingForClan(UUID clanId) {
        return events.values().stream().filter(e -> e.involves(clanId)).toList();
    }

    public java.util.concurrent.CompletableFuture<List<ClanEvent>> getRecentFinished(UUID clanId) {
        return plugin.getScheduler().supplyAsync(() -> repository.findRecentFinishedForClan(clanId, 10));
    }

    // ------------------------------------------------------------ أدوات البث

    private void broadcastMessage(RuntimeState state, String key, Map<String, String> placeholders) {
        for (EventParticipant p : state.participants.values()) {
            Player player = plugin.getServer().getPlayer(p.getPlayerUuid());
            if (player != null) plugin.getMessageService().send(player, key, placeholders);
        }
    }

    private void broadcastActionBar(RuntimeState state, String key, Map<String, String> placeholders) {
        for (EventParticipant p : state.participants.values()) {
            Player player = plugin.getServer().getPlayer(p.getPlayerUuid());
            if (player == null || !plugin.getPrefsService().get(player.getUniqueId()).hud()) continue;
            String raw = plugin.getMessageService().raw(player, key);
            for (var entry : placeholders.entrySet()) {
                raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(raw));
        }
    }

    private void broadcastTitle(RuntimeState state, String key, Map<String, String> placeholders) {
        for (EventParticipant p : state.participants.values()) {
            Player player = plugin.getServer().getPlayer(p.getPlayerUuid());
            if (player == null || !plugin.getPrefsService().get(player.getUniqueId()).hud()) continue;
            String raw = plugin.getMessageService().raw(player, key);
            for (var entry : placeholders.entrySet()) {
                raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            Component main = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
            player.showTitle(Title.title(main, Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ZERO)));
        }
    }
}
