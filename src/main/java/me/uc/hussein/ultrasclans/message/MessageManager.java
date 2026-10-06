package me.uc.hussein.ultrasclans.message;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * يحمّل رسائل كل اللغات المدعومة معًا (en + ar) حتى يستطيع كل لاعب اختيار
 * لغته الخاصة دون التأثير على الآخرين. اللغة الافتراضية للسيرفر من config.yml
 * (language) وتُستخدم لمن لم يختر لغة، وللرسائل التي لا مستلم محدد لها.
 * المفاتيح على شكل "file.key" (مثال: "clan.create-success").
 */
public final class MessageManager {

    public static final List<String> LANGUAGES = List.of("en", "ar");

    private static final String[] FILES = {
            "general", "clan", "invites", "requests", "members", "errors",
            "bank", "storage", "cp", "upgrades",
            "warps", "spawn", "tpa", "chat", "alliance", "tasks", "events", "settings", "prefs"
    };

    private final UltrasClansPlugin plugin;
    private final Map<String, Map<String, String>> byLanguage = new HashMap<>();
    private String defaultLanguage = "ar";
    private String prefixStandalone = "ULTRAS |";
    private String prefixChat = "&c&lULTRAS &8| &7";

    public MessageManager(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        byLanguage.clear();
        var cfg = plugin.getConfigManager().get();
        String configured = cfg.getString("language", "ar");
        defaultLanguage = LANGUAGES.contains(configured) ? configured : "en";
        prefixStandalone = cfg.getString("prefix-standalone", "ULTRAS |");
        prefixChat = cfg.getString("prefix-chat", "&c&lULTRAS &8| &7");

        for (String language : LANGUAGES) {
            Map<String, String> messages = new HashMap<>();
            for (String file : FILES) {
                String resourcePath = "messages/" + language + "/" + file + ".yml";
                File diskFile = new File(plugin.getDataFolder(), resourcePath);
                if (!diskFile.exists() && plugin.getResource(resourcePath) != null) {
                    diskFile.getParentFile().mkdirs();
                    plugin.saveResource(resourcePath, false);
                }
                if (!diskFile.exists()) {
                    continue;
                }
                FileConfiguration yaml = YamlConfiguration.loadConfiguration(diskFile);

                // دمج أي مفتاح جديد أُضيف في تحديث حديث من نسخة الـjar
                try (InputStream defStream = plugin.getResource(resourcePath)) {
                    if (defStream != null) {
                        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                                new InputStreamReader(defStream, StandardCharsets.UTF_8));
                        for (String key : defaults.getKeys(false)) {
                            if (!yaml.contains(key)) {
                                yaml.set(key, defaults.get(key));
                            }
                        }
                    }
                } catch (IOException ignored) {
                    // نستمر بالملف الموجود على القرص فقط
                }
                for (String key : yaml.getKeys(false)) {
                    messages.put(file + "." + key, yaml.getString(key, ""));
                }
            }
            byLanguage.put(language, messages);
        }
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public boolean isSupported(String language) {
        return LANGUAGES.contains(language);
    }

    /** النص الخام للغة محددة، مع الرجوع للغة السيرفر ثم الإنجليزية إن غاب المفتاح. */
    public String raw(String language, String key) {
        String value = lookup(language, key);
        if (value == null) value = lookup(defaultLanguage, key);
        if (value == null) value = lookup("en", key);
        return value != null ? value : "&c[missing message: " + key + "]";
    }

    public String raw(String key) {
        return raw(defaultLanguage, key);
    }

    private String lookup(String language, String key) {
        Map<String, String> messages = byLanguage.get(language);
        return messages == null ? null : messages.get(key);
    }

    public String get(String language, String key, Map<String, String> placeholders) {
        String result = raw(language, key).replace("{prefix}", prefixChat);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
        }
        return ColorUtil.colorize(result);
    }

    /** بلغة السيرفر الافتراضية (للمستلمين غير المحددين كالكونسول). */
    public String get(String key, Map<String, String> placeholders) {
        return get(defaultLanguage, key, placeholders);
    }

    public String get(String key) {
        return get(defaultLanguage, key, Map.of());
    }

    public String prefixStandalone() {
        return ColorUtil.colorize(prefixStandalone);
    }
}
