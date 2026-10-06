package me.uc.hussein.ultrasclans.service;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.JoinRequest;
import me.uc.hussein.ultrasclans.repository.RequestRepository;
import me.uc.hussein.ultrasclans.security.KeyedLock;
import me.uc.hussein.ultrasclans.util.Scheduler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class RequestService {

    public enum SendFailReason { ALREADY_PENDING, LIMIT_REACHED, ALREADY_IN_CLAN, DB_ERROR }
    public enum RespondFailReason { NOT_FOUND, EXPIRED, REQUESTER_ALREADY_IN_CLAN, DB_ERROR }

    private final UltrasClansPlugin plugin;
    private final ClanService clanService;
    private final MemberService memberService;
    private final RequestRepository requestRepo;
    private final Scheduler scheduler;
    private final KeyedLock<String> lock = new KeyedLock<>();

    public RequestService(UltrasClansPlugin plugin, ClanService clanService, MemberService memberService) {
        this.plugin = plugin;
        this.clanService = clanService;
        this.memberService = memberService;
        this.requestRepo = new RequestRepository(plugin.getDatabaseManager());
        this.scheduler = plugin.getScheduler();
    }

    public void send(UUID requesterUuid, UUID clanId,
                      java.util.function.Consumer<SendFailReason> onFail, Runnable onSuccess) {
        if (clanService.isInClan(requesterUuid)) {
            onFail.accept(SendFailReason.ALREADY_IN_CLAN);
            return;
        }
        String lockKey = clanId + ":" + requesterUuid;
        if (!lock.tryLock(lockKey)) {
            onFail.accept(SendFailReason.ALREADY_PENDING);
            return;
        }

        boolean allowDuplicates = plugin.getConfigManager().get().getBoolean("join-requests.allow-duplicate-requests", false);
        int maxPending = plugin.getConfigManager().get().getInt("join-requests.max-pending-per-clan", 30);
        long expiryMinutes = plugin.getConfigManager().get().getLong("join-requests.expiry-minutes", 60);

        scheduler.supplyAsync(() -> {
            if (!allowDuplicates && requestRepo.hasPendingRequest(clanId, requesterUuid)) {
                return "ALREADY_PENDING";
            }
            if (requestRepo.countPendingForClan(clanId) >= maxPending) {
                return "LIMIT_REACHED";
            }
            long now = System.currentTimeMillis();
            JoinRequest request = new JoinRequest(UUID.randomUUID(), clanId, requesterUuid,
                    now, now + (expiryMinutes * 60_000L), JoinRequest.Status.PENDING);
            requestRepo.insert(request);
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

    public CompletableFuture<List<JoinRequest>> listPendingForClan(UUID clanId) {
        return scheduler.supplyAsync(() -> requestRepo.findPendingForClan(clanId));
    }

    /** يقبل الطلب، ويُلغي تلقائيًا أي طلبات أخرى معلقة لنفس اللاعب لكلانات أخرى. */
    public void accept(UUID requestId, java.util.function.Consumer<RespondFailReason> onFail,
                        java.util.function.Consumer<UUID> onSuccessClanId) {
        scheduler.supplyAsync(() -> requestRepo.findById(requestId)).whenComplete((opt, error) -> {
            if (error != null) {
                scheduler.runSync(() -> onFail.accept(RespondFailReason.DB_ERROR));
                return;
            }
            if (opt.isEmpty() || opt.get().getStatus() != JoinRequest.Status.PENDING) {
                scheduler.runSync(() -> onFail.accept(RespondFailReason.NOT_FOUND));
                return;
            }
            JoinRequest request = opt.get();
            if (clanService.isInClan(request.getRequesterUuid())) {
                scheduler.runAsync(() -> requestRepo.updateStatus(requestId, JoinRequest.Status.DENIED));
                scheduler.runSync(() -> onFail.accept(RespondFailReason.REQUESTER_ALREADY_IN_CLAN));
                return;
            }
            if (request.isExpired()) {
                scheduler.runAsync(() -> requestRepo.updateStatus(requestId, JoinRequest.Status.EXPIRED));
                scheduler.runSync(() -> onFail.accept(RespondFailReason.EXPIRED));
                return;
            }

            scheduler.runAsync(() -> {
                requestRepo.updateStatus(requestId, JoinRequest.Status.ACCEPTED);
                requestRepo.denyOtherPendingForPlayer(request.getRequesterUuid(), requestId);
            }).whenComplete((v, updateErr) -> scheduler.runSync(() ->
                    memberService.join(request.getRequesterUuid(), request.getClanId(),
                            failReason -> onFail.accept(RespondFailReason.DB_ERROR),
                            restored -> onSuccessClanId.accept(request.getClanId()))));
        });
    }

    public CompletableFuture<Void> deny(UUID requestId) {
        return scheduler.runAsync(() -> requestRepo.updateStatus(requestId, JoinRequest.Status.DENIED));
    }
}
