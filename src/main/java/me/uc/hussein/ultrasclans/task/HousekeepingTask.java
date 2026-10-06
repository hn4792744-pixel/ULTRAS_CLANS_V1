package me.uc.hussein.ultrasclans.task;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.repository.HistoryRepository;
import me.uc.hussein.ultrasclans.repository.InviteRepository;
import me.uc.hussein.ultrasclans.repository.RequestRepository;

/**
 * مهمة دورية (كل 60 ثانية) تنظّف الدعوات وطلبات الانضمام والـ history
 * snapshots المنتهية الصلاحية. تعمل بالكامل على thread قاعدة البيانات
 * ولا تلمس Bukkit API إطلاقًا (قسم 59: لا loops كل tick، فقط عند الحاجة).
 */
public final class HousekeepingTask implements Runnable {

    private static final long PERIOD_TICKS = 20L * 60L; // كل دقيقة

    private final UltrasClansPlugin plugin;
    private final InviteRepository inviteRepo;
    private final RequestRepository requestRepo;
    private final HistoryRepository historyRepo;
    private final me.uc.hussein.ultrasclans.backup.BackupService backups;

    public HousekeepingTask(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.inviteRepo = new InviteRepository(plugin.getDatabaseManager());
        this.requestRepo = new RequestRepository(plugin.getDatabaseManager());
        this.historyRepo = new HistoryRepository(plugin.getDatabaseManager());
        this.backups = new me.uc.hussein.ultrasclans.backup.BackupService(plugin);
    }

    public void start() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this, PERIOD_TICKS, PERIOD_TICKS);
    }

    @Override
    public void run() {
        try {
            int expiredInvites = inviteRepo.expireOverdue();
            int expiredRequests = requestRepo.expireOverdue();
            int purgedHistory = historyRepo.purgeExpired();
            plugin.getTaskService().flushDirty();
            backups.runIfDue();
            int retentionDays = plugin.getConfigManager().get().getInt("logs.retention-days", 28);
            if (retentionDays > 0) {
                long cutoff = System.currentTimeMillis() - retentionDays * 24L * 60L * 60L * 1000L;
                plugin.getClanService().getAuditRepository().purgeOlderThan(cutoff);
            }
            if (expiredInvites > 0 || expiredRequests > 0 || purgedHistory > 0) {
                plugin.getLogger().fine("Housekeeping: expired " + expiredInvites + " invites, "
                        + expiredRequests + " requests, purged " + purgedHistory + " history snapshots.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Housekeeping task failed: " + e.getMessage());
        }
    }
}
