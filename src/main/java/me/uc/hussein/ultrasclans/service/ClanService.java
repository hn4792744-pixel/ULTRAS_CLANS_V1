package me.uc.hussein.ultrasclans.service;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.hook.EconomyHook;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanMember;
import me.uc.hussein.ultrasclans.model.ClanRankDefinition;
import me.uc.hussein.ultrasclans.rank.RankTemplate;
import me.uc.hussein.ultrasclans.repository.AuditLogRepository;
import me.uc.hussein.ultrasclans.repository.ClanRepository;
import me.uc.hussein.ultrasclans.repository.CooldownRepository;
import me.uc.hussein.ultrasclans.repository.MemberRepository;
import me.uc.hussein.ultrasclans.repository.RankRepository;
import me.uc.hussein.ultrasclans.security.KeyedLock;
import me.uc.hussein.ultrasclans.security.NameValidator;
import me.uc.hussein.ultrasclans.util.Cooldown;
import me.uc.hussein.ultrasclans.util.Scheduler;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * الخدمة المركزية لإدارة دورة حياة الكلان: الإنشاء، الحذف، التعديل،
 * ونقل الملكية. تحتفظ بذاكرة تخزين مؤقت (cache) متزامنة مع قاعدة
 * البيانات لتفادي استعلامات متكررة على الـmain thread.
 */
public final class ClanService {

    private final UltrasClansPlugin plugin;
    private final ClanRepository clanRepo;
    private final MemberRepository memberRepo;
    private final RankRepository rankRepo;
    private final CooldownRepository cooldownRepo;
    private final AuditLogRepository auditRepo;
    private final RankTemplate rankTemplate;
    private final EconomyHook economy;
    private final Scheduler scheduler;

    private final Map<UUID, Clan> clansById = new ConcurrentHashMap<>();
    private final Map<String, UUID> clanIdByNameLower = new ConcurrentHashMap<>();
    private final Map<UUID, ClanMember> membersByUuid = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> memberUuidsByClan = new ConcurrentHashMap<>();
    private final Map<UUID, List<ClanRankDefinition>> ranksByClan = new ConcurrentHashMap<>();

    private final Cooldown createCooldown = new Cooldown();
    private final Cooldown rejoinCooldown = new Cooldown();
    private final KeyedLock<String> nameLock = new KeyedLock<>();

    public ClanService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.clanRepo = new ClanRepository(plugin.getDatabaseManager());
        this.memberRepo = new MemberRepository(plugin.getDatabaseManager());
        this.rankRepo = new RankRepository(plugin.getDatabaseManager());
        this.cooldownRepo = new CooldownRepository(plugin.getDatabaseManager());
        this.auditRepo = new AuditLogRepository(plugin.getDatabaseManager());
        this.rankTemplate = plugin.getRankTemplate();
        this.economy = plugin.getEconomyHook();
        this.scheduler = plugin.getScheduler();
    }

    // ------------------------------------------------------------
    // تحميل الكاش عند بدء التشغيل
    // ------------------------------------------------------------

    public CompletableFuture<Void> loadAll() {
        return scheduler.runAsync(() -> {
            List<Clan> clans = clanRepo.findAll();
            for (Clan clan : clans) {
                clansById.put(clan.getId(), clan);
                clanIdByNameLower.put(clan.getName().toLowerCase(Locale.ROOT), clan.getId());
                memberUuidsByClan.put(clan.getId(), ConcurrentHashMap.newKeySet());
                ranksByClan.put(clan.getId(), new ArrayList<>(rankRepo.findByClan(clan.getId())));
            }
            List<ClanMember> members = memberRepo.findAll();
            for (ClanMember member : members) {
                membersByUuid.put(member.getUuid(), member);
                memberUuidsByClan.computeIfAbsent(member.getClanId(), k -> ConcurrentHashMap.newKeySet())
                        .add(member.getUuid());
            }
            plugin.getLogger().info("Loaded " + clans.size() + " clans and " + members.size() + " members into cache.");
        });
    }

    // ------------------------------------------------------------
    // قراءات الكاش (Cache reads - آمنة على أي thread)
    // ------------------------------------------------------------

    public Optional<Clan> getClan(UUID clanId) {
        return Optional.ofNullable(clansById.get(clanId));
    }

    public Optional<Clan> getClanByName(String name) {
        UUID id = clanIdByNameLower.get(name.toLowerCase(Locale.ROOT));
        return id == null ? Optional.empty() : getClan(id);
    }

    public Optional<ClanMember> getMember(UUID playerUuid) {
        return Optional.ofNullable(membersByUuid.get(playerUuid));
    }

    public Optional<Clan> getPlayerClan(UUID playerUuid) {
        ClanMember member = membersByUuid.get(playerUuid);
        return member == null ? Optional.empty() : getClan(member.getClanId());
    }

    public boolean isInClan(UUID playerUuid) {
        return membersByUuid.containsKey(playerUuid);
    }

    public List<ClanMember> getMembers(UUID clanId) {
        Set<UUID> uuids = memberUuidsByClan.getOrDefault(clanId, Set.of());
        List<ClanMember> result = new ArrayList<>();
        for (UUID uuid : uuids) {
            ClanMember member = membersByUuid.get(uuid);
            if (member != null) {
                result.add(member);
            }
        }
        return result;
    }

    public List<ClanRankDefinition> getRanks(UUID clanId) {
        return Collections.unmodifiableList(ranksByClan.getOrDefault(clanId, List.of()));
    }

    public Optional<ClanRankDefinition> getRank(UUID clanId, String rankKey) {
        return getRanks(clanId).stream().filter(r -> r.getRankKey().equalsIgnoreCase(rankKey)).findFirst();
    }

    public List<Clan> getAllClansSnapshot() {
        return new ArrayList<>(clansById.values());
    }

    // ------------------------------------------------------------
    // إنشاء الكلان
    // ------------------------------------------------------------

    /**
     * يشغّل تدفق إنشاء الكلان الكامل بأمان (تحقق -> خصم -> إدراج -> refund عند الفشل).
     * يُستدعى من الـmain thread فقط (لأنه يستخدم Vault ويحتاج بيانات اللاعب المتصل).
     */
    public void createClan(Player player, String rawName, java.util.function.Consumer<CreateClanResult> callback) {
        UUID uuid = player.getUniqueId();

        if (isInClan(uuid)) {
            callback.accept(CreateClanResult.fail("ALREADY_IN_CLAN"));
            return;
        }

        long cooldownSeconds = plugin.getConfigManager().get().getLong("clan-creation.cooldown-seconds", 60);
        if (!player.hasPermission("ultras.clans.bypass.cooldown") && createCooldown.isOnCooldown(uuid)) {
            callback.accept(CreateClanResult.fail("COOLDOWN:" + createCooldown.remainingSeconds(uuid)));
            return;
        }

        NameValidator validator = new NameValidator(
                plugin.getConfigManager().get().getConfigurationSection("clan-creation.name"));
        NameValidator.ValidationResult validation = validator.validate(rawName);
        if (!validation.valid()) {
            callback.accept(CreateClanResult.fail("NAME_INVALID:" + validation.reasonKey()));
            return;
        }

        String nameLower = rawName.toLowerCase(Locale.ROOT);
        if (!nameLock.tryLock(nameLower)) {
            // كلان آخر بنفس الاسم قيد الإنشاء في هذه اللحظة بالضبط
            callback.accept(CreateClanResult.fail("NAME_TAKEN"));
            return;
        }

        double price = plugin.getConfigManager().get().getDouble("clan-creation.price", 1000.0);
        boolean economyEnabled = plugin.getConfigManager().get().getBoolean("economy.enabled", true);

        scheduler.supplyAsync(() -> clanRepo.existsByNameLower(nameLower)).whenComplete((exists, error) -> {
            if (error != null) {
                nameLock.unlock(nameLower);
                scheduler.runSync(() -> callback.accept(CreateClanResult.fail("DB_ERROR")));
                return;
            }
            if (Boolean.TRUE.equals(exists)) {
                nameLock.unlock(nameLower);
                scheduler.runSync(() -> callback.accept(CreateClanResult.fail("NAME_TAKEN")));
                return;
            }

            scheduler.runSync(() -> {
                try {
                    // الخصم يتم على الـmain thread (تفاعل مع Vault)
                    if (economyEnabled) {
                        if (!economy.has(player, price)) {
                            callback.accept(CreateClanResult.fail("NOT_ENOUGH_MONEY"));
                            return;
                        }
                        if (!economy.withdraw(player, price)) {
                            callback.accept(CreateClanResult.fail("NOT_ENOUGH_MONEY"));
                            return;
                        }
                    }

                    finishCreate(player, rawName, nameLower, price, economyEnabled, callback);
                } finally {
                    nameLock.unlock(nameLower);
                }
            });
        });
    }

    private void finishCreate(Player player, String rawName, String nameLower, double price,
                               boolean economyEnabled, java.util.function.Consumer<CreateClanResult> callback) {
        UUID uuid = player.getUniqueId();
        UUID clanId = UUID.randomUUID();
        long now = System.currentTimeMillis();

        var defaults = plugin.getConfigManager().get().getConfigurationSection("clan-defaults");
        String color = defaults != null ? defaults.getString("color", "WHITE") : "WHITE";
        boolean isPublic = defaults == null || defaults.getBoolean("public", true);
        int memberCapacity = defaults != null ? defaults.getInt("member-capacity", 10) : 10;
        double bankCapacity = defaults != null ? defaults.getDouble("bank-capacity", 50000) : 50000;
        int storageLevel = defaults != null ? defaults.getInt("storage-level", 1) : 1;
        int warpCapacity = defaults != null ? defaults.getInt("warp-capacity", 1) : 1;
        int allianceCapacity = defaults != null ? defaults.getInt("alliance-capacity", 1) : 1;

        Clan clan = new Clan(clanId, rawName, uuid, color, isPublic, memberCapacity,
                0.0, bankCapacity, 0L, 1, storageLevel, warpCapacity, allianceCapacity, now);

        List<ClanRankDefinition> defaultRanks = rankTemplate.buildDefaultRanksFor(clanId);
        String ownerRankKey = rankTemplate.getOwnerRankKey();

        ClanMember ownerMember = new ClanMember(uuid, clanId, ownerRankKey, 1, 0, 0, 0, 0, now, now);

        String correlationId = UUID.randomUUID().toString();

        scheduler.runAsync(() -> {
            clanRepo.insert(clan);
            for (ClanRankDefinition rank : defaultRanks) {
                rankRepo.insert(rank);
            }
            memberRepo.insert(ownerMember);
            cooldownRepo.setLastCreateAttemptAt(uuid, now);
            auditRepo.log(correlationId, uuid, clanId, "CLAN_CREATED", String.valueOf(price),
                    null, rawName, "command");
        }).whenComplete((v, error) -> scheduler.runSync(() -> {
            if (error != null) {
                plugin.getLogger().severe("Failed to create clan '" + rawName + "': " + error.getMessage());
                if (economyEnabled) {
                    economy.deposit(player, price);
                }
                callback.accept(CreateClanResult.fail("DB_ERROR"));
                return;
            }

            // تحديث الكاش فقط بعد نجاح الحفظ في قاعدة البيانات
            clansById.put(clanId, clan);
            clanIdByNameLower.put(nameLower, clanId);
            ranksByClan.put(clanId, new ArrayList<>(defaultRanks));
            membersByUuid.put(uuid, ownerMember);
            memberUuidsByClan.computeIfAbsent(clanId, k -> ConcurrentHashMap.newKeySet()).add(uuid);

            createCooldown.set(uuid, plugin.getConfigManager().get().getLong("clan-creation.cooldown-seconds", 60));
            callback.accept(CreateClanResult.success(clan));
        }));
    }

    // ------------------------------------------------------------
    // حذف الكلان (حل كامل)
    // ------------------------------------------------------------

    public CompletableFuture<Void> deleteClan(UUID clanId, String reasonAction, UUID actorUuid) {
        List<ClanMember> members = getMembers(clanId);
        String correlationId = UUID.randomUUID().toString();

        var db = plugin.getDatabaseManager();
        return scheduler.runAsync(() -> {
            memberRepo.deleteByClan(clanId);
            rankRepo.deleteByClan(clanId);
            // كل ما يرتبط بالكلان يُحذف معه (قسم 71): دعوات، طلبات، محتوى المخزن
            new me.uc.hussein.ultrasclans.repository.InviteRepository(db).deleteByClan(clanId);
            new me.uc.hussein.ultrasclans.repository.RequestRepository(db).deleteByClan(clanId);
            new me.uc.hussein.ultrasclans.repository.StorageRepository(db).deleteByClan(clanId);
            new me.uc.hussein.ultrasclans.repository.WarpRepository(db).deleteByClan(clanId);
            new me.uc.hussein.ultrasclans.repository.SpawnRepository(db).delete(clanId);
            new me.uc.hussein.ultrasclans.repository.TaskRepository(db).deleteByClan(clanId);
            clanRepo.delete(clanId);
            auditRepo.log(correlationId, actorUuid, clanId, reasonAction, null, null, null, "system");
        }).thenCompose(v -> plugin.getAllianceService().removeClan(clanId)).thenRun(() -> scheduler.runSync(() -> {
            Clan clan = clansById.remove(clanId);
            if (clan != null) {
                clanIdByNameLower.remove(clan.getName().toLowerCase(Locale.ROOT));
            }
            ranksByClan.remove(clanId);
            memberUuidsByClan.remove(clanId);
            for (ClanMember member : members) {
                membersByUuid.remove(member.getUuid());
            }
            plugin.getTaskService().removeClan(clanId);
        }));
    }

    // ------------------------------------------------------------
    // تحديثات عامة على الكلان (لون، اسم، خصوصية...) - تُطبَّق على الكاش
    // فورًا (تفاؤليًا) ثم تُحفظ بشكل غير متزامن.
    // ------------------------------------------------------------

    public CompletableFuture<Void> persist(Clan clan) {
        return scheduler.runAsync(() -> clanRepo.update(clan));
    }

    public Cooldown getCreateCooldown() {
        return createCooldown;
    }

    public Cooldown getRejoinCooldown() {
        return rejoinCooldown;
    }

    public CooldownRepository getCooldownRepository() {
        return cooldownRepo;
    }

    public AuditLogRepository getAuditRepository() {
        return auditRepo;
    }

    public MemberRepository getMemberRepository() {
        return memberRepo;
    }

    public RankRepository getRankRepository() {
        return rankRepo;
    }

    public ClanRepository getClanRepository() {
        return clanRepo;
    }

    // حزمة الوصول للكاش الداخلي من MemberService (نفس الحزمة منطقيًا لكن الفئات منفصلة)
    Map<UUID, ClanMember> membersByUuidRef() {
        return membersByUuid;
    }

    Map<UUID, Set<UUID>> memberUuidsByClanRef() {
        return memberUuidsByClan;
    }

    OfflinePlayer resolveOffline(UUID uuid) {
        return plugin.getServer().getOfflinePlayer(uuid);
    }

    // ------------------------------------------------------------
    // نقل الملكية وإعادة الضبط (قسم 63/45) - يُستدعيان من الأوامر والإدارة
    // ------------------------------------------------------------

    public enum TransferFail { NOT_MEMBER, ALREADY_OWNER }

    /**
     * ينقل الملكية: المالك الجديد يأخذ رتبة Owner، والقديم يصبح عضوًا عاديًا
     * (لا يمكن وجود مالكين). يُستدعى على الـmain thread. يُرجع null عند النجاح.
     */
    public TransferFail transferOwnership(Clan clan, UUID newOwnerUuid, UUID actorUuid, String source) {
        ClanMember newMember = membersByUuid.get(newOwnerUuid);
        if (newMember == null || !newMember.getClanId().equals(clan.getId())) {
            return TransferFail.NOT_MEMBER;
        }
        if (clan.getOwnerUuid().equals(newOwnerUuid)) {
            return TransferFail.ALREADY_OWNER;
        }
        UUID oldOwner = clan.getOwnerUuid();
        ClanMember oldMember = membersByUuid.get(oldOwner);
        synchronized (clan) {
            clan.setOwnerUuid(newOwnerUuid);
        }
        newMember.setRankKey(rankTemplate.getOwnerRankKey());
        if (oldMember != null) {
            oldMember.setRankKey(rankTemplate.getDefaultMemberRankKey());
        }
        String correlationId = UUID.randomUUID().toString();
        scheduler.runAsync(() -> {
            clanRepo.update(clan);
            memberRepo.update(newMember);
            if (oldMember != null) memberRepo.update(oldMember);
            auditRepo.log(correlationId, actorUuid, clan.getId(), "OWNER_TRANSFER", null,
                    oldOwner.toString(), newOwnerUuid.toString(), source);
        });
        return null;
    }

    /** يعيد الكلان لحالة جديدة مع بقاء الاسم والمالك (قسم 62). يُستدعى على الـmain thread. */
    public void resetClan(Clan clan, UUID actorUuid) {
        var defaults = plugin.getConfigManager().get().getConfigurationSection("clan-defaults");
        synchronized (clan) {
            clan.setCp(0);
            clan.setClanLevel(1);
            clan.setBankBalance(0);
            clan.setMemberCapacity(defaults != null ? defaults.getInt("member-capacity", 10) : 10);
            clan.setBankCapacity(defaults != null ? defaults.getDouble("bank-capacity", 50000) : 50000);
            clan.setStorageLevel(defaults != null ? defaults.getInt("storage-level", 1) : 1);
            clan.setWarpCapacity(defaults != null ? defaults.getInt("warp-capacity", 1) : 1);
            clan.setAllianceCapacity(defaults != null ? defaults.getInt("alliance-capacity", 1) : 1);
        }
        String correlationId = UUID.randomUUID().toString();
        scheduler.runAsync(() -> {
            clanRepo.update(clan);
            auditRepo.log(correlationId, actorUuid, clan.getId(), "CLAN_RESET", null, null, null, "admin");
        });
    }

    /**
     * يعيد تسمية الكلان بنفس قواعد الإنشاء (قسم 64). يُستدعى على الـmain thread.
     * @return null عند النجاح، وإلا رمز الفشل: NAME_INVALID:&lt;reason&gt; أو NAME_TAKEN
     */
    public String rename(Clan clan, String newName) {
        NameValidator validator = new NameValidator(
                plugin.getConfigManager().get().getConfigurationSection("clan-creation.name"));
        NameValidator.ValidationResult validation = validator.validate(newName);
        if (!validation.valid()) {
            return "NAME_INVALID:" + validation.reasonKey();
        }
        String lower = newName.toLowerCase(Locale.ROOT);
        UUID existing = clanIdByNameLower.get(lower);
        if (existing != null && !existing.equals(clan.getId())) {
            return "NAME_TAKEN";
        }
        String oldName;
        synchronized (clan) {
            oldName = clan.getName();
            clan.setName(newName);
        }
        clanIdByNameLower.remove(oldName.toLowerCase(Locale.ROOT));
        clanIdByNameLower.put(lower, clan.getId());
        String correlationId = UUID.randomUUID().toString();
        scheduler.runAsync(() -> {
            clanRepo.update(clan);
            auditRepo.log(correlationId, clan.getOwnerUuid(), clan.getId(), "CLAN_RENAMED", null, oldName, newName, "command");
        }).whenComplete((v, error) -> {
            if (error != null) {
                scheduler.runSync(() -> {
                    synchronized (clan) { clan.setName(oldName); }
                    clanIdByNameLower.remove(lower);
                    clanIdByNameLower.put(oldName.toLowerCase(Locale.ROOT), clan.getId());
                });
            }
        });
        return null;
    }
}
