package me.uc.hussein.ultrasclans.task;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;

/**
 * يعمل كل دقيقة على الـmain thread (يحتاج قراءة اللاعبين المتصلين).
 * لا علاقة له بالحفظ في قاعدة البيانات - فقط يحدّث تقدم مهام
 * PLAYTIME_MINUTES وMEMBERS_ONLINE في الذاكرة (قسم 59: لا loops كل tick).
 */
public final class TaskTickTask implements Runnable {

    private static final long PERIOD_TICKS = 20L * 60L;

    private final UltrasClansPlugin plugin;

    public TaskTickTask(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, PERIOD_TICKS, PERIOD_TICKS);
    }

    @Override
    public void run() {
        for (Clan clan : plugin.getClanService().getAllClansSnapshot()) {
            int online = 0;
            for (var member : plugin.getClanService().getMembers(clan.getId())) {
                if (plugin.getServer().getPlayer(member.getUuid()) != null) {
                    online++;
                }
            }
            if (online > 0) {
                plugin.getTaskService().incrementProgress(clan, TaskType.PLAYTIME_MINUTES, 1);
            }
            plugin.getTaskService().recordPeak(clan, TaskType.MEMBERS_ONLINE, online);
        }
    }
}
