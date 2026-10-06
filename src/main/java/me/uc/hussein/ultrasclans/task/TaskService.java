package me.uc.hussein.ultrasclans.task;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanTaskProgress;
import me.uc.hussein.ultrasclans.model.ClanTaskProgress.Period;
import me.uc.hussein.ultrasclans.repository.TaskRepository;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * يدير تقدم مهام الكلان الثلاث (يومية/أسبوعية/شهرية) - قسم 31. التقدم
 * مشترك بين كل أعضاء الكلان (وليس فرديًا)، ويُحفظ بشكل مؤجل (دفعة كل
 * دقيقة عبر HousekeepingTask) بدل كتابة قاعدة بيانات عند كل حدث.
 */
public final class TaskService {

    public enum ClaimResult { SUCCESS, ALREADY_CLAIMED, NOT_COMPLETE }

    /** لقطة للعرض في الـGUI/الأوامر: القالب النشط + حالة التقدم الحالية. */
    public record TaskView(TaskTemplate template, long progress, boolean claimed) {
        public int percent() {
            if (template == null || template.target() <= 0) return 0;
            return (int) Math.min(100, (progress * 100) / template.target());
        }

        public boolean isComplete() {
            return template != null && progress >= template.target();
        }
    }

    private final UltrasClansPlugin plugin;
    private final TaskRepository repository;
    private final TaskTemplateLoader templates;

    private final Map<UUID, EnumMap<Period, ClanTaskProgress>> cache = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyClans = ConcurrentHashMap.newKeySet();

    public TaskService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.repository = new TaskRepository(plugin.getDatabaseManager());
        this.templates = new TaskTemplateLoader(plugin);
    }

    public void reloadTemplates() {
        templates.load();
    }

    public CompletableFuture<Void> loadAll() {
        return plugin.getScheduler().runAsync(() -> {
            for (var row : repository.findAll()) {
                cache.computeIfAbsent(row.clanId(), k -> new EnumMap<>(Period.class)).put(row.period(), row.progress());
            }
        });
    }

    private ClanTaskProgress stateFor(UUID clanId, Period period) {
        return cache.computeIfAbsent(clanId, k -> new EnumMap<>(Period.class))
                .computeIfAbsent(period, p -> new ClanTaskProgress(templates.currentPeriodKey(p), 0, false));
    }

    /**
     * يتحقق من دخول فترة جديدة، وإن كانت المهمة السابقة مكتملة وغير
     * مُستلَمة، تُمنح مكافأتها تلقائيًا لـCP الكلان (قسم 31).
     */
    private void applyRollover(Clan clan, Period period) {
        ClanTaskProgress state = stateFor(clan.getId(), period);
        long currentKey = templates.currentPeriodKey(period);

        long staleKey = -1;
        long staleProgress = 0;
        boolean staleClaimed = false;
        boolean changed = false;

        synchronized (state) {
            if (state.getPeriodKey() != currentKey) {
                staleKey = state.getPeriodKey();
                staleProgress = state.getProgress();
                staleClaimed = state.isClaimed();
                state.setPeriodKey(currentKey);
                state.setProgress(0);
                state.setClaimed(false);
                changed = true;
            }
        }

        if (changed) {
            dirtyClans.add(clan.getId());
            if (!staleClaimed) {
                TaskTemplate oldTemplate = templates.templateFor(period, staleKey);
                if (oldTemplate != null && staleProgress >= oldTemplate.target()) {
                    plugin.getPointsService().addCpForClan(clan, oldTemplate.rewardCp(), "Task: " + oldTemplate.name());
                    for (var member : plugin.getClanService().getMembers(clan.getId())) {
                        Player online = plugin.getServer().getPlayer(member.getUuid());
                        if (online != null) {
                            plugin.getMessageService().send(online, "tasks.auto-claimed",
                                    Map.of("task", oldTemplate.name()));
                        }
                    }
                }
            }
        }
    }

    public TaskView view(Clan clan, Period period) {
        applyRollover(clan, period);
        ClanTaskProgress state = stateFor(clan.getId(), period);
        TaskTemplate template = templates.templateFor(period, state.getPeriodKey());
        synchronized (state) {
            return new TaskView(template, state.getProgress(), state.isClaimed());
        }
    }

    /** يُستدعى من مستمعي القتل/التعدين/البناء/البنك - يزيد تقدم أي مهمة نشطة مطابقة للنوع. */
    public void incrementProgress(Clan clan, TaskType type, long amount) {
        if (clan == null || amount <= 0) return;
        for (Period period : Period.values()) {
            applyRollover(clan, period);
            ClanTaskProgress state = stateFor(clan.getId(), period);
            TaskTemplate template = templates.templateFor(period, state.getPeriodKey());
            if (template == null || template.type() != type) continue;

            synchronized (state) {
                if (state.isClaimed()) continue;
                long updated = Math.min(template.target(), state.getProgress() + amount);
                if (updated != state.getProgress()) {
                    state.setProgress(updated);
                    dirtyClans.add(clan.getId());
                }
            }
        }
    }

    /** لـMEMBERS_ONLINE: يسجّل أعلى قيمة رأيناها هذه الفترة، وليس تراكميًا. */
    public void recordPeak(Clan clan, TaskType type, long currentValue) {
        if (clan == null) return;
        for (Period period : Period.values()) {
            applyRollover(clan, period);
            ClanTaskProgress state = stateFor(clan.getId(), period);
            TaskTemplate template = templates.templateFor(period, state.getPeriodKey());
            if (template == null || template.type() != type) continue;

            synchronized (state) {
                if (state.isClaimed()) continue;
                long updated = Math.min(template.target(), Math.max(state.getProgress(), currentValue));
                if (updated != state.getProgress()) {
                    state.setProgress(updated);
                    dirtyClans.add(clan.getId());
                }
            }
        }
    }

    public void claim(Player player, Clan clan, Period period, java.util.function.Consumer<ClaimResult> onResult,
                       java.util.function.BiConsumer<TaskTemplate, Long> onSuccess) {
        applyRollover(clan, period);
        ClanTaskProgress state = stateFor(clan.getId(), period);
        TaskTemplate template = templates.templateFor(period, state.getPeriodKey());
        if (template == null) {
            onResult.accept(ClaimResult.NOT_COMPLETE);
            return;
        }

        boolean success = false;
        synchronized (state) {
            if (state.isClaimed()) {
                onResult.accept(ClaimResult.ALREADY_CLAIMED);
                return;
            }
            if (state.getProgress() < template.target()) {
                onResult.accept(ClaimResult.NOT_COMPLETE);
                return;
            }
            state.setClaimed(true);
            success = true;
        }
        if (success) {
            dirtyClans.add(clan.getId());
            plugin.getPointsService().addCpForClan(clan, template.rewardCp(), "Task: " + template.name());
            onSuccess.accept(template, template.rewardCp());
        }
    }

    /** يُستدعى من HousekeepingTask (thread قاعدة البيانات) كل دقيقة تقريبًا. */
    public void flushDirty() {
        Set<UUID> snapshot = Set.copyOf(dirtyClans);
        for (UUID clanId : snapshot) {
            dirtyClans.remove(clanId);
            EnumMap<Period, ClanTaskProgress> periods = cache.get(clanId);
            if (periods == null) continue;
            for (var entry : periods.entrySet()) {
                ClanTaskProgress snap;
                synchronized (entry.getValue()) {
                    snap = new ClanTaskProgress(entry.getValue().getPeriodKey(),
                            entry.getValue().getProgress(), entry.getValue().isClaimed());
                }
                try {
                    repository.save(clanId, entry.getKey(), snap);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save tasks for clan " + clanId + ": " + e.getMessage());
                    dirtyClans.add(clanId);
                }
            }
        }
    }

    public void removeClan(UUID clanId) {
        cache.remove(clanId);
        dirtyClans.remove(clanId);
    }
}
