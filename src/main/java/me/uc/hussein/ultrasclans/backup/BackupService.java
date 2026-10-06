package me.uc.hussein.ultrasclans.backup;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.database.DatabaseType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;

/**
 * نسخ احتياطي يومي/أسبوعي لقاعدة SQLite (قسم 84) عبر VACUUM INTO (لقطة متسقة
 * حتى أثناء الكتابة). MySQL لا يُنسخ من هنا (استخدم أدوات القاعدة نفسها).
 * يُستدعى من HousekeepingTask على thread قاعدة البيانات - لا Bukkit API هنا.
 */
public final class BackupService {

    private final UltrasClansPlugin plugin;
    private volatile LocalDate lastBackupDay;

    public BackupService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void runIfDue() {
        var cfg = plugin.getConfigManager().get();
        if (!cfg.getBoolean("backups.enabled", true)
                || plugin.getDatabaseManager().getType() != DatabaseType.SQLITE) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (today.equals(lastBackupDay)) {
            return;
        }
        File dailyDir = new File(plugin.getDataFolder(), "backups/daily");
        File weeklyDir = new File(plugin.getDataFolder(), "backups/weekly");
        dailyDir.mkdirs();
        weeklyDir.mkdirs();

        File daily = new File(dailyDir, "data-" + today + ".db");
        try {
            if (!daily.exists()) {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     Statement st = conn.createStatement()) {
                    st.execute("VACUUM INTO '" + daily.getAbsolutePath().replace("'", "''") + "'");
                }
                plugin.getLogger().info("Database backup created: " + daily.getName());
            }
            if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
                File weekly = new File(weeklyDir, "data-week-" + today + ".db");
                if (!weekly.exists() && daily.exists()) {
                    Files.copy(daily.toPath(), weekly.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            prune(dailyDir, cfg.getInt("backups.daily-keep", 7));
            prune(weeklyDir, cfg.getInt("backups.weekly-keep", 4));
            lastBackupDay = today;
        } catch (Exception e) {
            plugin.getLogger().warning("Backup failed: " + e.getMessage());
        }
    }

    private void prune(File dir, int keep) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".db"));
        if (files == null || files.length <= keep) {
            return;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (int i = keep; i < files.length; i++) {
            files[i].delete();
        }
    }
}
