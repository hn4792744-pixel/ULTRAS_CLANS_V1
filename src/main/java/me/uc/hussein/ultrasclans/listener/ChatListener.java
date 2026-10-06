package me.uc.hussein.ultrasclans.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.Optional;

/**
 * يعالج شات الكلان (تحويل الرسالة إلى الأعضاء فقط) ووسم الكلان في
 * الشات العام. يعمل بعد ChatInputListener (الذي يلغي الرسالة أولًا إن
 * كان اللاعب في انتظار إدخال نصي) ولا يتدخل إن كان الحدث مُلغى مسبقًا،
 * ولا يفرض تنسيقه إلا إذا فعّل اللاعب الوسم صراحة (قسم 70).
 */
public final class ChatListener implements Listener {

    private final UltrasClansPlugin plugin;

    public ChatListener(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncChatEvent event) {
        if (event.isCancelled()) {
            return; // ChatInputListener تكفّل بها (اسم كلان جديد، بحث...)
        }
        Player player = event.getPlayer();
        Optional<Clan> clanOpt = plugin.getClanService().getPlayerClan(player.getUniqueId());

        if (plugin.getChatService().isClanChatEnabled(player.getUniqueId())) {
            handleClanChat(event, player, clanOpt);
            return;
        }

        if (clanOpt.isPresent() && plugin.getChatService().isTagEnabled(player.getUniqueId())
                && plugin.getConfigManager().loadYaml("chat.yml").getBoolean("show-clan-in-chat", true)) {
            applyTagRenderer(event, clanOpt.get());
        }
    }

    private void handleClanChat(AsyncChatEvent event, Player player, Optional<Clan> clanOpt) {
        event.setCancelled(true);

        if (clanOpt.isEmpty()) {
            plugin.getChatService().setClanChat(player.getUniqueId(), false);
            plugin.getMessageService().send(player, "clan.not-in-clan");
            return;
        }
        boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_CHAT)
                || plugin.getPermissionService().isOwner(player.getUniqueId());
        if (!allowed) {
            plugin.getChatService().setClanChat(player.getUniqueId(), false);
            plugin.getMessageService().send(player, "chat.no-permission-chat");
            return;
        }

        Clan clan = clanOpt.get();
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        String format = plugin.getConfigManager().loadYaml("chat.yml")
                .getString("clan-chat-format", "&8[&bCLAN&8] {clan_color}{clan} &8| &f{player}&8: &7{message}")
                .replace("{clan_color}", "&" + shortColorCode(clan.getColor()))
                .replace("{clan}", clan.getName())
                .replace("{player}", player.getName())
                .replace("{message}", text);
        Component rendered = LegacyComponentSerializer.legacyAmpersand().deserialize(format);

        for (var member : plugin.getClanService().getMembers(clan.getId())) {
            Player online = plugin.getServer().getPlayer(member.getUuid());
            if (online != null) {
                online.sendMessage(rendered);
            }
        }
    }

    private void applyTagRenderer(AsyncChatEvent event, Clan clan) {
        String tagFormat = plugin.getConfigManager().loadYaml("chat.yml").getString("tag-format", "&8[{clan_color}{clan}&8] &r")
                .replace("{clan_color}", "&" + shortColorCode(clan.getColor()))
                .replace("{clan}", clan.getName());
        Component tag = LegacyComponentSerializer.legacyAmpersand().deserialize(tagFormat);

        event.renderer((source, sourceDisplayName, message, viewer) ->
                tag.append(sourceDisplayName).append(Component.text(": ")).append(message));
    }

    /** يحوّل اسم لون Bukkit (مثال WHITE) إلى رمز & (مثال f) - يُستخدم في تنسيقات الشات. */
    private String shortColorCode(String bukkitColorName) {
        try {
            return String.valueOf(org.bukkit.ChatColor.valueOf(bukkitColorName.toUpperCase()).getChar());
        } catch (IllegalArgumentException e) {
            return "f";
        }
    }
}
