package me.uc.hussein.ultrasclans.task;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.ClanTaskProgress;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * يحمّل قوالب المهام العشرة لكل فترة من tasks.yml، ويحدد القالب النشط
 * حاليًا بشكل حتمي بدون أي حالة مخزّنة (قسم 31: التدوير التلقائي).
 */
public final class TaskTemplateLoader {

    private final UltrasClansPlugin plugin;
    private List<TaskTemplate> daily = new ArrayList<>();
    private List<TaskTemplate> weekly = new ArrayList<>();
    private List<TaskTemplate> monthly = new ArrayList<>();

    public TaskTemplateLoader(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        FileConfiguration yaml = plugin.getConfigManager().loadYaml("tasks.yml");
        daily = readList(yaml, "daily");
        weekly = readList(yaml, "weekly");
        monthly = readList(yaml, "monthly");
    }

    @SuppressWarnings("unchecked")
    private List<TaskTemplate> readList(FileConfiguration yaml, String path) {
        List<TaskTemplate> result = new ArrayList<>();
        List<?> raw = yaml.getList(path);
        if (raw == null) {
            return result;
        }
        for (Object obj : raw) {
            if (obj instanceof java.util.Map<?, ?> map) {
                var m = (java.util.Map<String, Object>) map;
                try {
                    String name = String.valueOf(m.get("name"));
                    TaskType type = TaskType.valueOf(String.valueOf(m.get("type")));
                    long target = ((Number) m.get("target")).longValue();
                    long reward = ((Number) m.get("reward-cp")).longValue();
                    result.add(new TaskTemplate(name, type, target, reward));
                } catch (Exception e) {
                    plugin.getLogger().warning("Skipping invalid task template in " + path + ": " + e.getMessage());
                }
            }
        }
        return result;
    }

    /** المفتاح الحتمي الحالي للفترة (يوم/أسبوع/شهر) - يتغيّر فقط عندما تبدأ فترة جديدة فعليًا. */
    public long currentPeriodKey(ClanTaskProgress.Period period) {
        String tz = plugin.getConfigManager().get().getString("timezone", "");
        ZoneId zone = (tz == null || tz.isBlank()) ? ZoneId.systemDefault() : ZoneId.of(tz);
        LocalDate today = LocalDate.now(zone);
        return switch (period) {
            case DAILY -> today.toEpochDay();
            case WEEKLY -> Math.floorDiv(today.toEpochDay(), 7);
            case MONTHLY -> (long) today.getYear() * 12 + today.getMonthValue();
        };
    }

    public TaskTemplate templateFor(ClanTaskProgress.Period period, long periodKey) {
        List<TaskTemplate> list = switch (period) {
            case DAILY -> daily;
            case WEEKLY -> weekly;
            case MONTHLY -> monthly;
        };
        if (list.isEmpty()) {
            return null;
        }
        int index = (int) Math.floorMod(periodKey, list.size());
        return list.get(index);
    }
}
