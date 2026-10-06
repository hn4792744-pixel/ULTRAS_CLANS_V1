package me.uc.hussein.ultrasclans.service;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanMember;
import me.uc.hussein.ultrasclans.model.MemberHistorySnapshot;
import me.uc.hussein.ultrasclans.rank.RankTemplate;
import me.uc.hussein.ultrasclans.repository.HistoryRepository;
import me.uc.hussein.ultrasclans.repository.MemberRepository;
import me.uc.hussein.ultrasclans.util.Scheduler;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * يدير دخول وخروج وطرد وترقية/تخفيض الأعضاء، بالإضافة إلى نظام
 * استرجاع البيانات خلال 3 أيام من المغادرة (قسم 16 من المواصفات).
 */
public final class MemberService {

    private final UltrasClansPlugin plugin;
    private final ClanService clanService;
    private final MemberRepository memberRepo;
    private final HistoryRepository historyRepo;
    private final RankTemplate rankTemplate;
    private final Scheduler scheduler;

    public MemberService(UltrasClansPlugin plugin, ClanService clanService) {
        this.plugin = plugin;
        this.clanService = clanService;
        this.memberRepo = clanService.getMemberRepository();
        this.historyRepo = new HistoryRepository(plugin.getDatabaseManager());
        this.rankTemplate = plugin.getRankTemplate();
        this.scheduler = plugin.getScheduler();
    }

    public enum JoinFailReason { ALREADY_IN_CLAN, CLAN_FULL, REJOIN_COOLDOWN, CLAN_NOT_FOUND, DB_ERROR }

    /**
     * يضيف لاعبًا لكلان (بعد قبول دعوة أو طلب، أو الانضمام المباشر لكلان عام).
     * يسترجع تلقائيًا اللقطة السابقة إذا كان اللاعب يعود لنفس الكلان خلال المهلة.
     */
    public void join(UUID playerUuid, UUID clanId, java.util.function.Consumer<JoinFailReason> onFail,
                      java.util.function.Consumer<Boolean> onSuccess) {
        if (clanService.isInClan(playerUuid)) {
            onFail.accept(JoinFailReason.ALREADY_IN_CLAN);
            return;
        }
        Optional<Clan> clanOpt = clanService.getClan(clanId);
        if (clanOpt.isEmpty()) {
            onFail.accept(JoinFailReason.CLAN_NOT_FOUND);
            return;
        }
        Clan clan = clanOpt.get();
        if (clanService.getMembers(clanId).size() >= clan.getMemberCapacity()) {
            onFail.accept(JoinFailReason.CLAN_FULL);
            return;
        }
        var onlinePlayer = plugin.getServer().getPlayer(playerUuid);
        boolean bypassCooldown = onlinePlayer != null && onlinePlayer.hasPermission("ultras.clans.bypass.cooldown");
        if (!bypassCooldown && clanService.getRejoinCooldown().isOnCooldown(playerUuid)) {
            onFail.accept(JoinFailReason.REJOIN_COOLDOWN);
            return;
        }

        long now = System.currentTimeMillis();
        String correlationId = UUID.randomUUID().toString();

        scheduler.supplyAsync(() -> historyRepo.find(playerUuid)).whenComplete((snapshotOpt, err) -> {
            boolean restore = err == null && snapshotOpt.isPresent()
                    && !snapshotOpt.get().isExpired() && snapshotOpt.get().getClanId().equals(clanId);

            ClanMember member;
            if (restore) {
                MemberHistorySnapshot s = snapshotOpt.get();
                member = new ClanMember(playerUuid, clanId, s.getRankKey(), s.getMemberLevel(), s.getMemberXp(),
                        s.getKills(), s.getDeaths(), s.getContribution(), now, now);
            } else {
                member = new ClanMember(playerUuid, clanId, rankTemplate.getDefaultMemberRankKey(),
                        1, 0, 0, 0, 0, now, now);
            }
            final ClanMember finalMember = member;
            final boolean finalRestore = restore;

            scheduler.runAsync(() -> {
                memberRepo.insert(finalMember);
                if (finalRestore) {
                    historyRepo.deleteFor(playerUuid);
                }
                clanService.getAuditRepository().log(correlationId, playerUuid, clanId, "MEMBER_JOINED",
                        null, null, null, "service");
            }).whenComplete((v, dbErr) -> scheduler.runSync(() -> {
                if (dbErr != null) {
                    onFail.accept(JoinFailReason.DB_ERROR);
                    return;
                }
                clanService.membersByUuidRef().put(playerUuid, finalMember);
                clanService.memberUuidsByClanRef()
                        .computeIfAbsent(clanId, k -> ConcurrentHashMap.newKeySet())
                        .add(playerUuid);
                onSuccess.accept(finalRestore);
            }));
        });
    }

    public enum LeaveOutcome { LEFT, CLAN_DISBANDED }

    /**
     * يُخرج لاعبًا من كلانه الحالي. إذا كان هو المالك ولم ينقل الملكية،
     * يُحل الكلان بالكامل تلقائيًا (قسم 17).
     */
    public CompletableFuture<LeaveOutcome> leave(UUID playerUuid) {
        Optional<ClanMember> memberOpt = clanService.getMember(playerUuid);
        if (memberOpt.isEmpty()) {
            return CompletableFuture.completedFuture(LeaveOutcome.LEFT);
        }
        ClanMember member = memberOpt.get();
        UUID clanId = member.getClanId();
        Optional<Clan> clanOpt = clanService.getClan(clanId);
        boolean isOwner = clanOpt.isPresent() && clanOpt.get().getOwnerUuid().equals(playerUuid);

        if (isOwner) {
            return clanService.deleteClan(clanId, "OWNER_LEFT_DISBAND", playerUuid)
                    .thenApply(v -> LeaveOutcome.CLAN_DISBANDED);
        }

        return saveSnapshotAndRemove(member, clanOpt.map(Clan::getName).orElse("Unknown"))
                .thenApply(v -> LeaveOutcome.LEFT);
    }

    /** يُستخدم أيضًا من الطرد (kick) حيث لا يُطبَّق cooldown إعادة الانضمام على المطرود بنفس الطريقة. */
    public CompletableFuture<Void> kick(UUID playerUuid) {
        Optional<ClanMember> memberOpt = clanService.getMember(playerUuid);
        if (memberOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        ClanMember member = memberOpt.get();
        String clanName = clanService.getClan(member.getClanId()).map(Clan::getName).orElse("Unknown");
        return saveSnapshotAndRemove(member, clanName);
    }

    private CompletableFuture<Void> saveSnapshotAndRemove(ClanMember member, String clanName) {
        long now = System.currentTimeMillis();
        long retentionDays = plugin.getConfigManager().get().getLong("membership.history-retention-days", 3);
        long expiresAt = now + (retentionDays * 24L * 60L * 60L * 1000L);

        MemberHistorySnapshot snapshot = new MemberHistorySnapshot(
                member.getUuid(), member.getClanId(), clanName, member.getRankKey(),
                member.getMemberLevel(), member.getMemberXp(), member.getKills(),
                member.getDeaths(), member.getContribution(), now, expiresAt);

        return scheduler.runAsync(() -> {
            historyRepo.save(snapshot);
            memberRepo.delete(member.getUuid());
            clanService.getCooldownRepository().setLastLeaveAt(member.getUuid(), now);
        }).thenRun(() -> scheduler.runSync(() -> {
            clanService.membersByUuidRef().remove(member.getUuid());
            var set = clanService.memberUuidsByClanRef().get(member.getClanId());
            if (set != null) {
                set.remove(member.getUuid());
            }
            long rejoinCooldownMinutes = plugin.getConfigManager().get()
                    .getLong("membership.rejoin-cooldown-minutes", 10);
            clanService.getRejoinCooldown().set(member.getUuid(), rejoinCooldownMinutes * 60);
        }));
    }

    public CompletableFuture<Void> setRank(UUID playerUuid, String newRankKey) {
        ClanMember member = clanService.getMember(playerUuid).orElseThrow();
        member.setRankKey(newRankKey);
        return scheduler.runAsync(() -> memberRepo.update(member));
    }
}
