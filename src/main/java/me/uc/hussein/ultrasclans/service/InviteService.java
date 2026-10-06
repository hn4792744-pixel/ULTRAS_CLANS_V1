package me.uc.hussein.ultrasclans.service;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.ClanInvite;
import me.uc.hussein.ultrasclans.repository.InviteRepository;
import me.uc.hussein.ultrasclans.security.KeyedLock;
import me.uc.hussein.ultrasclans.util.Scheduler;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class InviteService {

    public enum SendFailReason { RECEIVER_IN_CLAN, ALREADY_PENDING, LIMIT_REACHED, SENDER_NOT_IN_CLAN, DB_ERROR }
    public enum RespondFailReason { NOT_FOUND, EXPIRED, ALREADY_IN_CLAN, DB_ERROR }

    private final UltrasClansPlugin plugin;
    private final ClanService clanService;
    private final MemberService memberService;
    private final InviteRepository inviteRepo;
    private final Scheduler scheduler;
    private final KeyedLock<String> lock = new KeyedLock<>();

    public InviteService(UltrasClansPlugin plugin, ClanService clanService, MemberService memberService) {
        this.plugin = plugin;
        this.clanService = clanService;
        this.memberService = memberService;
        this.inviteRepo = new InviteRepository(plugin.getDatabaseManager());
        this.scheduler = plugin.getScheduler();
    }

    public void send(UUID senderUuid, UUID clanId, UUID receiverUuid,
                      java.util.function.Consumer<SendFailReason> onFail, Runnable onSuccess) {
        if (clanService.isInClan(receiverUuid)) {
            onFail.accept(SendFailReason.RECEIVER_IN_CLAN);
            return;
        }
        String lockKey = clanId + ":" + receiverUuid;
        if (!lock.tryLock(lockKey)) {
            onFail.accept(SendFailReason.ALREADY_PENDING);
            return;
        }

        int maxPending = plugin.getConfigManager().get().getInt("invites.max-pending-per-clan", 20);
        long expiryMinutes = plugin.getConfigManager().get().getLong("invites.expiry-minutes", 5);

        scheduler.supplyAsync(() -> {
            if (inviteRepo.hasPendingInvite(clanId, receiverUuid)) {
                return "ALREADY_PENDING";
            }
            if (inviteRepo.countPendingForClan(clanId) >= maxPending) {
                return "LIMIT_REACHED";
            }
            long now = System.currentTimeMillis();
            ClanInvite invite = new ClanInvite(UUID.randomUUID(), clanId, senderUuid, receiverUuid,
                    now, now + (expiryMinutes * 60_000L), ClanInvite.Status.PENDING);
            inviteRepo.insert(invite);
            return "OK";
        }).whenComplete((result, error) -> scheduler.runSync(() -> {
            lock.unlock(lockKey);
            if (error != null) {
                onFail.accept(SendFailReason.DB_ERROR);
                return;
            }
            switch (result) {
                case "ALREADY_PENDING" -> onFail.accept(SendFailReason.ALREADY_PENDING);
                case "LIMIT_REACHED" -> onFail.accept(SendFailReason.LIMIT_REACHED);
                default -> onSuccess.run();
            }
        }));
    }

    public CompletableFuture<List<ClanInvite>> listPendingForPlayer(UUID playerUuid) {
        return scheduler.supplyAsync(() -> inviteRepo.findPendingForReceiver(playerUuid));
    }

    public CompletableFuture<Optional<ClanInvite>> findPendingForClanAndReceiver(UUID clanId, UUID receiverUuid) {
        return scheduler.supplyAsync(() -> inviteRepo.findPendingForClanAndReceiver(clanId, receiverUuid));
    }

    public void accept(UUID inviteId, UUID responderUuid,
                        java.util.function.Consumer<RespondFailReason> onFail,
                        java.util.function.Consumer<UUID> onSuccessClanId) {
        if (clanService.isInClan(responderUuid)) {
            onFail.accept(RespondFailReason.ALREADY_IN_CLAN);
            return;
        }
        scheduler.supplyAsync(() -> inviteRepo.findById(inviteId)).whenComplete((opt, error) -> {
            if (error != null) {
                scheduler.runSync(() -> onFail.accept(RespondFailReason.DB_ERROR));
                return;
            }
            if (opt.isEmpty() || opt.get().getStatus() != ClanInvite.Status.PENDING
                    || !opt.get().getReceiverUuid().equals(responderUuid)) {
                scheduler.runSync(() -> onFail.accept(RespondFailReason.NOT_FOUND));
                return;
            }
            ClanInvite invite = opt.get();
            if (invite.isExpired()) {
                scheduler.runAsync(() -> inviteRepo.updateStatus(inviteId, ClanInvite.Status.EXPIRED));
                scheduler.runSync(() -> onFail.accept(RespondFailReason.EXPIRED));
                return;
            }
            scheduler.runAsync(() -> inviteRepo.updateStatus(inviteId, ClanInvite.Status.ACCEPTED))
                    .whenComplete((v, updateErr) -> scheduler.runSync(() ->
                            memberService.join(responderUuid, invite.getClanId(),
                                    failReason -> onFail.accept(RespondFailReason.DB_ERROR),
                                    restored -> onSuccessClanId.accept(invite.getClanId()))));
        });
    }

    public CompletableFuture<Void> deny(UUID inviteId) {
        return scheduler.runAsync(() -> inviteRepo.updateStatus(inviteId, ClanInvite.Status.DENIED));
    }
}
