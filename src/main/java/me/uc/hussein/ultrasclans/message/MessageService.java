package me.uc.hussein.ultrasclans.message;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.PlayerPrefs;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * نقطة الإرسال الوحيدة لرسائل البلاجن إلى اللاعبين: تُطبّق لغة اللاعب الشخصية، وتحترم
 * "إيقاف رسائل البلاجن" الخاص به، دون أي تأثير على بقية الأعضاء.
 * عناوين الواجهات (text) لا تتأثر بإيقاف الرسائل لأنها ليست رسائل شات.
 */
public final class MessageService {

    private final UltrasClansPlugin plugin;

    public MessageService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public String languageOf(UUID uuid) {
        PlayerPrefs prefs = plugin.getPrefsService().get(uuid);
        String lang = prefs.language();
        return (lang != null && plugin.getMessageManager().isSupported(lang))
                ? lang : plugin.getMessageManager().getDefaultLanguage();
    }

    public void send(CommandSender to, String key) {
        send(to, key, Map.of());
    }

    public void send(CommandSender to, String key, Map<String, String> placeholders) {
        if (to instanceof Player player) {
            if (!plugin.getPrefsService().get(player.getUniqueId()).messages()) {
                return;
            }
            player.sendMessage(plugin.getMessageManager().get(languageOf(player.getUniqueId()), key, placeholders));
        } else {
            to.sendMessage(plugin.getMessageManager().get(key, placeholders));
        }
    }

    /** نص جاهز (بأكواد & ) غير مترجَم، كرسائل الاستخدام - يحترم إيقاف الرسائل أيضًا. */
    public void sendLiteral(CommandSender to, String text) {
        if (to instanceof Player player && !plugin.getPrefsService().get(player.getUniqueId()).messages()) {
            return;
        }
        to.sendMessage(me.uc.hussein.ultrasclans.util.ColorUtil.colorize(text));
    }

    public String text(Player player, String key) {
        return text(player, key, Map.of());
    }

    public String text(Player player, String key, Map<String, String> placeholders) {
        return plugin.getMessageManager().get(languageOf(player.getUniqueId()), key, placeholders);
    }

    public String raw(Player player, String key) {
        return plugin.getMessageManager().raw(languageOf(player.getUniqueId()), key);
    }
}
