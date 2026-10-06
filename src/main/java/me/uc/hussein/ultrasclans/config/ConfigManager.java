package me.uc.hussein.ultrasclans.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * يدير تحميل وحفظ وإعادة تحميل ملف config.yml الرئيسي، بالإضافة إلى
 * أي ملف YAML آخر داخل مجلد البلاجن (ranks.yml, permissions.yml...).
 * يتحقق دائمًا من نسخة الـconfig (config-version) لدعم الترقية المستقبلية.
 */
public final class ConfigManager {

    private final Plugin plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        applyDefaults();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        applyDefaults();
    }

    /**
     * يدمج القيم الافتراضية من الملف المضمّن في الـjar فوق ملف المستخدم،
     * بحيث لو أضفنا خيارًا جديدًا في تحديث قادم، يظهر تلقائيًا دون
     * حذف تخصيصات اللاعب الحالية.
     */
    private void applyDefaults() {
        try (InputStream defStream = plugin.getResource("config.yml")) {
            if (defStream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defStream, StandardCharsets.UTF_8));
                config.setDefaults(defaults);
                config.options().copyDefaults(false);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to merge config defaults: " + e.getMessage());
        }
    }

    /**
     * يقوم بتحميل ملف YAML إضافي من مجلد البلاجن (مع إنشائه من موارد الـjar
     * إذا لم يكن موجودًا مسبقًا).
     */
    public FileConfiguration loadYaml(String resourceName) {
        File file = new File(plugin.getDataFolder(), resourceName);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(resourceName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration get() {
        return config;
    }
}
