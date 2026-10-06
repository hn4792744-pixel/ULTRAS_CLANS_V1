package me.uc.hussein.ultrasclans.tpa;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.util.Cooldown;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TpaService {

    public enum SendFail { NOT_SAME_CLAN, SELF, OFFLINE, COOLDOWN, TARGET_DISABLED }
    public enum RespondFail { NONE_PENDING, EXPIRED, SENDER_OFFLINE }

    private final UltrasClansPlugin plugin;
    private final Map<UUID, List<TpaRequest>> pendingByReceiver = new ConcurrentHashMap<>();
    private final Cooldown sendCooldown = new Cooldown();

    public TpaService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void send(Player sender, Player target, TpaRequest.Mode mode,
                      java.util.function.Consumer<SendFail> onFail, Runnable onSuccess) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            onFail.accept(SendFail.SELF);
            return;
        }
        var senderClan = plugin.getClanService().getPlayerClan(sender.getUniqueId());
        var targetClan = plugin.getClanService().getPlayerClan(target.getUniqueId());
        boolean sameClan = senderClan.isPresent() && targetClan.isPresent()
                && senderClan.get().getId().equals(targetClan.get().getId());
        if (!sameClan) {
            onFail.accept(SendFail.NOT_SAME_CLAN);
            return;
        }
        if (!plugin.getPrefsService().get(target.getUniqueId()).tpa()) {
            onFail.accept(SendFail.TARGET_DISABLED);
            return;
        }
        if (!sender.hasPermission("ultras.clans.bypass.cooldown") && sendCooldown.isOnCooldown(sender.getUniqueId())) {
            onFail.accept(SendFail.COOLDOWN);
            return;
        }

        var config = plugin.getConfigManager().loadYaml("warps.yml");
        long expirySeconds = config.getLong("tpa.request-expiry-seconds", 60);
        long cooldownSeconds = config.getLong("tpa.cooldown-seconds", 30);

        TpaRequest request = new TpaRequest(sender.getUniqueId(), target.getUniqueId(), mode,
                System.currentTimeMillis() + expirySeconds * 1000L);
        pendingByReceiver.computeIfAbsent(target.getUniqueId(), k -> new CopyOnWriteArrayList<>()).add(request);
        sendCooldown.set(sender.getUniqueId(), cooldownSeconds);

        onSuccess.run();
    }

    /** يقبل آخر طلب صالح، أو الطلب القادم من لاعب معيّن إن حُدِّد اسمه. */
    public void accept(Player accepter, UUID filterSenderUuidOrNull,
                        java.util.function.Consumer<RespondFail> onFail,
                        java.util.function.BiConsumer<Player, TpaRequest> onSuccess) {
        List<TpaRequest> list = pendingByReceiver.get(accepter.getUniqueId());
        TpaRequest found = pickRequest(list, filterSenderUuidOrNull);
        if (found == null) {
            onFail.accept(RespondFail.NONE_PENDING);
            return;
        }
        list.remove(found);
        if (found.isExpired()) {
            onFail.accept(RespondFail.EXPIRED);
            return;
        }
        Player sender = plugin.getServer().getPlayer(found.getSenderUuid());
        if (sender == null) {
            onFail.accept(RespondFail.SENDER_OFFLINE);
            return;
        }
        onSuccess.accept(sender, found);
    }

    public void deny(Player accepter, UUID filterSenderUuidOrNull,
                      java.util.function.Consumer<RespondFail> onFail, java.util.function.Consumer<Player> onDenied) {
        List<TpaRequest> list = pendingByReceiver.get(accepter.getUniqueId());
        TpaRequest found = pickRequest(list, filterSenderUuidOrNull);
        if (found == null) {
            onFail.accept(RespondFail.NONE_PENDING);
            return;
        }
        list.remove(found);
        Player sender = plugin.getServer().getPlayer(found.getSenderUuid());
        onDenied.accept(sender);
    }

    private TpaRequest pickRequest(List<TpaRequest> list, UUID filterSenderUuidOrNull) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (filterSenderUuidOrNull == null) {
            return list.get(list.size() - 1); // آخر طلب مُرسَل
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).getSenderUuid().equals(filterSenderUuidOrNull)) {
                return list.get(i);
            }
        }
        return null;
    }

    /** يرسل الطلب ويتكفّل بكل الرسائل/الأصوات - نقطة استدعاء واحدة من الأوامر والـGUI. */
    public void sendAndNotify(Player sender, Player target, TpaRequest.Mode mode) {
        send(sender, target, mode, fail -> {
            String key = switch (fail) {
                case NOT_SAME_CLAN -> "tpa.not-same-clan";
                case SELF -> "tpa.self";
                case OFFLINE -> "tpa.offline";
                case COOLDOWN -> "tpa.cooldown";
                case TARGET_DISABLED -> "prefs.blocked-tpa";
            };
            plugin.getMessageService().send(sender, key, Map.of("player", target.getName()));
        }, () -> {
            boolean here = mode == TpaRequest.Mode.HERE;
            plugin.getMessageService().send(sender, here ? "tpa.sent-here" : "tpa.sent",
                    Map.of("player", target.getName()));
            plugin.getMessageService().send(target, here ? "tpa.received-here" : "tpa.received",
                    Map.of("player", sender.getName()));
            plugin.playSound(target, "request");
        });
    }

    public void clearFor(UUID uuid) {
        pendingByReceiver.remove(uuid);
    }
}
