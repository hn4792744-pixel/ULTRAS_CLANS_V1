package me.uc.hussein.ultrasclans.alliance;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.AllianceInvite;
import me.uc.hussein.ultrasclans.model.AllianceSettings;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanAlliance;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import me.uc.hussein.ultrasclans.repository.AllianceInviteRepository;
import me.uc.hussein.ultrasclans.repository.AllianceRepository;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * إدارة كاملة لدورة حياة التحالفات: الاقتراح، القبول، الرفض، الحل،
 * وإعدادات الصلاحيات المتبادلة (قسم 27). كل تحالف جديد يبدأ بلا أي
 * صلاحيات ممنوحة ("Alliance = no access" افتراضيًا) عدا منع الـPvP.
 */
public final class AllianceService {

    public enum SendFail { SELF, ALREADY_ALLIED, ALREADY_PENDING, LIMIT_REACHED, NO_PERMISSION, TARGET_DISABLED }
    public enum RespondFail { NONE_PENDING, LIMIT_REACHED }

    private final UltrasClansPlugin plugin;
    private final AllianceRepository repository;
    private final AllianceInviteRepository inviteRepository;

    /** clanId -> قائمة تحالفاته الحالية (كاش في الذاكرة، مُحمَّل عند الإقلاع). */
    private final Map<UUID, List<ClanAlliance>> alliancesByClan = new ConcurrentHashMap<>();
    /** allianceId -> (الكلان المانح -> إعداداته) */
    private final Map<UUID, Map<UUID, AllianceSettings>> settingsCache = new ConcurrentHashMap<>();

    public AllianceService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.repository = new AllianceRepository(plugin.getDatabaseManager());
        this.inviteRepository = new AllianceInviteRepository(plugin.getDatabaseManager());
    }

    public CompletableFuture<Void> loadAll() {
        return plugin.getScheduler().runAsync(() -> {
            for (Clan clan : plugin.getClanService().getAllClansSnapshot()) {
                List<ClanAlliance> list = repository.findByClan(clan.getId());
                for (ClanAlliance alliance : list) {
                    // addToCacheIfAbsent يتكفّل بمنع الإدراج المزدوج (نفس التحالف يظهر من جهة كل كلان)
                    addToCacheIfAbsent(alliance);
                    Map<UUID, AllianceSettings> settings = new ConcurrentHashMap<>();
                    settings.put(alliance.getClanA(), repository.loadSettings(alliance.getId(), alliance.getClanA()));
                    settings.put(alliance.getClanB(), repository.loadSettings(alliance.getId(), alliance.getClanB()));
                    settingsCache.put(alliance.getId(), settings);
                }
            }
        });
    }

    private void addToCacheIfAbsent(ClanAlliance alliance) {
        List<ClanAlliance> listA = alliancesByClan.computeIfAbsent(alliance.getClanA(), k -> new ArrayList<>());
        if (listA.stream().noneMatch(a -> a.getId().equals(alliance.getId()))) {
            listA.add(alliance);
        }
        List<ClanAlliance> listB = alliancesByClan.computeIfAbsent(alliance.getClanB(), k -> new ArrayList<>());
        if (listB.stream().noneMatch(a -> a.getId().equals(alliance.getId()))) {
            listB.add(alliance);
        }
    }

    public List<ClanAlliance> getAlliances(UUID clanId) {
        return List.copyOf(alliancesByClan.getOrDefault(clanId, List.of()));
    }

    public boolean areAllied(UUID clanA, UUID clanB) {
        return getAlliances(clanA).stream().anyMatch(a -> a.involves(clanB));
    }

    public Optional<ClanAlliance> getAlliance(UUID clanA, UUID clanB) {
        return getAlliances(clanA).stream().filter(a -> a.involves(clanB)).findFirst();
    }

    public AllianceSettings getSettings(UUID allianceId, UUID granterClanId) {
        return settingsCache.getOrDefault(allianceId, Map.of())
                .getOrDefault(granterClanId, AllianceSettings.defaults());
    }

    /** هل clanId يسمح لحليفه بصلاحية معيّنة؟ يفحص إعدادات clanId نفسه (ما يمنحه هو، لا ما يطلبه). */
    public boolean isGranted(UUID granterClanId, UUID alliedClanId, java.util.function.Predicate<AllianceSettings> check) {
        Optional<ClanAlliance> allianceOpt = getAlliance(granterClanId, alliedClanId);
        if (allianceOpt.isEmpty()) return false;
        return check.test(getSettings(allianceOpt.get().getId(), granterClanId));
    }

    // ------------------------------------------------------------ إرسال / قبول / رفض

    public void sendInvite(Player sender, Clan senderClan, Clan targetClan,
                            java.util.function.Consumer<SendFail> onFail, Runnable onSuccess) {
        if (senderClan.getId().equals(targetClan.getId())) {
            onFail.accept(SendFail.SELF);
            return;
        }
        boolean allowed = plugin.getPermissionService().hasPermission(sender.getUniqueId(), ClanPermission.CLAN_ALLIANCE_MANAGE)
                || plugin.getPermissionService().isOwner(sender.getUniqueId());
        if (!allowed) {
            onFail.accept(SendFail.NO_PERMISSION);
            return;
        }
        if (areAllied(senderClan.getId(), targetClan.getId())) {
            onFail.accept(SendFail.ALREADY_ALLIED);
            return;
        }
        if (!plugin.getPrefsService().anyManagerAccepts(targetClan, ClanPermission.CLAN_ALLIANCE_MANAGE,
                me.uc.hussein.ultrasclans.model.PlayerPrefs.Setting.ALLIANCE)) {
            onFail.accept(SendFail.TARGET_DISABLED);
            return;
        }
        int absoluteMax = plugin.getConfigManager().loadYaml("alliances.yml").getInt("max-alliances", 5);
        if (getAlliances(senderClan.getId()).size() >= Math.min(absoluteMax, senderClan.getAllianceCapacity())) {
            onFail.accept(SendFail.LIMIT_REACHED);
            return;
        }

        plugin.getScheduler().supplyAsync(() ->
                inviteRepository.hasPendingEitherDirection(senderClan.getId(), targetClan.getId())
        ).whenComplete((pending, error) -> plugin.getScheduler().runSync(() -> {
            if (error != null) return;
            if (Boolean.TRUE.equals(pending)) {
                onFail.accept(SendFail.ALREADY_PENDING);
                return;
            }
            long expiryMinutes = plugin.getConfigManager().loadYaml("alliances.yml").getLong("invite-expiry-minutes", 60);
            long now = System.currentTimeMillis();
            AllianceInvite invite = new AllianceInvite(UUID.randomUUID(), senderClan.getId(), targetClan.getId(),
                    sender.getUniqueId(), now, now + expiryMinutes * 60_000L, AllianceInvite.Status.PENDING);

            plugin.getScheduler().runAsync(() -> inviteRepository.insert(invite))
                    .whenComplete((v, insertError) -> plugin.getScheduler().runSync(() -> {
                        if (insertError == null) onSuccess.run();
                    }));
        }));
    }

    public void accept(Player accepter, Clan accepterClan, Clan proposerClan,
                        java.util.function.Consumer<RespondFail> onFail, Runnable onSuccess) {
        int absoluteMax = plugin.getConfigManager().loadYaml("alliances.yml").getInt("max-alliances", 5);
        if (getAlliances(accepterClan.getId()).size() >= Math.min(absoluteMax, accepterClan.getAllianceCapacity())) {
            onFail.accept(RespondFail.LIMIT_REACHED);
            return;
        }

        plugin.getScheduler().supplyAsync(() ->
                inviteRepository.findPendingBetween(proposerClan.getId(), accepterClan.getId())
        ).whenComplete((opt, error) -> {
            if (error != null || opt.isEmpty()) {
                plugin.getScheduler().runSync(() -> onFail.accept(RespondFail.NONE_PENDING));
                return;
            }
            AllianceInvite invite = opt.get();
            ClanAlliance alliance = canonical(proposerClan.getId(), accepterClan.getId());

            plugin.getScheduler().runAsync(() -> {
                inviteRepository.updateStatus(invite.getId(), AllianceInvite.Status.ACCEPTED);
                repository.insert(alliance);
            }).whenComplete((v, insertError) -> plugin.getScheduler().runSync(() -> {
                if (insertError != null) return;
                addToCacheIfAbsent(alliance);
                Map<UUID, AllianceSettings> settings = new ConcurrentHashMap<>();
                settings.put(alliance.getClanA(), AllianceSettings.defaults());
                settings.put(alliance.getClanB(), AllianceSettings.defaults());
                settingsCache.put(alliance.getId(), settings);
                onSuccess.run();
            }));
        });
    }

    public CompletableFuture<Boolean> deny(Clan accepterClan, Clan proposerClan) {
        return plugin.getScheduler().supplyAsync(() ->
                inviteRepository.findPendingBetween(proposerClan.getId(), accepterClan.getId())
        ).thenCompose(opt -> {
            if (opt.isEmpty()) {
                return CompletableFuture.completedFuture(false);
            }
            return plugin.getScheduler().runAsync(() ->
                    inviteRepository.updateStatus(opt.get().getId(), AllianceInvite.Status.DENIED)
            ).thenApply(v -> true);
        });
    }

    public CompletableFuture<Void> disband(UUID allianceId, UUID clanAId, UUID clanBId) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        plugin.getScheduler().runAsync(() -> repository.delete(allianceId)).whenComplete((v, error) ->
                plugin.getScheduler().runSync(() -> {
                    // التعديل على الكاش يجب أن يحصل على الـmain thread دائمًا (لا يُعدَّل
                    // من thread قاعدة البيانات بينما قد تقرأه واجهة مفتوحة في نفس اللحظة)
                    var listA = alliancesByClan.get(clanAId);
                    if (listA != null) listA.removeIf(a -> a.getId().equals(allianceId));
                    var listB = alliancesByClan.get(clanBId);
                    if (listB != null) listB.removeIf(a -> a.getId().equals(allianceId));
                    settingsCache.remove(allianceId);
                    if (error != null) {
                        result.completeExceptionally(error);
                    } else {
                        result.complete(null);
                    }
                }));
        return result;
    }

    public void updateSettings(UUID allianceId, UUID granterClanId, AllianceSettings settings) {
        settingsCache.computeIfAbsent(allianceId, k -> new ConcurrentHashMap<>()).put(granterClanId, settings);
        plugin.getScheduler().runAsync(() -> repository.saveSettings(allianceId, granterClanId, settings));
    }

    public CompletableFuture<Void> removeClan(UUID clanId) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        plugin.getScheduler().runAsync(() -> {
            repository.deleteAllForClan(clanId);
            inviteRepository.deleteByClan(clanId);
        }).whenComplete((v, error) -> plugin.getScheduler().runSync(() -> {
            // التعديل على الكاش يحصل دائمًا على الـmain thread (راجع disband())
            List<ClanAlliance> list = getAlliances(clanId);
            alliancesByClan.remove(clanId);
            for (ClanAlliance alliance : list) {
                var other = alliancesByClan.get(alliance.otherClan(clanId));
                if (other != null) other.removeIf(a -> a.getId().equals(alliance.getId()));
                settingsCache.remove(alliance.getId());
            }
            if (error != null) {
                result.completeExceptionally(error);
            } else {
                result.complete(null);
            }
        }));
        return result;
    }

    private ClanAlliance canonical(UUID clanX, UUID clanY) {
        UUID first = clanX.compareTo(clanY) <= 0 ? clanX : clanY;
        UUID second = first.equals(clanX) ? clanY : clanX;
        return new ClanAlliance(UUID.randomUUID(), first, second, System.currentTimeMillis());
    }
}
