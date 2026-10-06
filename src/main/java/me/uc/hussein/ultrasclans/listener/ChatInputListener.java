package me.uc.hussein.ultrasclans.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.PendingInputManager;
import me.uc.hussein.ultrasclans.service.CreateClanResult;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;

/**
 * يعترض رسائل الشات فقط للاعبين الذين طلب منهم البلاجن كتابة شيء
 * (اسم كلان جديد، بحث عن كلان...). لا يؤثر إطلاقًا على شات اللاعبين
 * الآخرين أو أي بلاجن شات آخر (قسم 6 و70).
 */
public final class ChatInputListener implements Listener {

    private final UltrasClansPlugin plugin;

    public ChatInputListener(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PendingInputManager.PendingInput pending = plugin.getGuiManager().pendingInput().get(player.getUniqueId());
        if (pending == null) {
            return;
        }

        // نلغي الرسالة بالكامل حتى لا تُبث لبقية السيرفر، ونعالجها كأمر داخلي
        event.setCancelled(true);
        String raw = PlainTextComponentSerializer.plainText().serialize(event.message());
        plugin.getGuiManager().pendingInput().clear(player.getUniqueId());

        plugin.getScheduler().runSync(() -> handle(player, pending.type(), raw.trim()));
    }

    private void handle(Player player, PendingInputManager.InputType type, String text) {
        switch (type) {
            case CREATE_CLAN_NAME -> handleCreateClanName(player, text);
            case SEARCH_CLANS -> handleSearchClans(player, text);
            case RENAME_CLAN -> handleRenameClan(player, text);
            case BANK_DEPOSIT_CUSTOM -> handleBankCustom(player, text,
                    me.uc.hussein.ultrasclans.gui.holder.BankGuiHolder.Mode.DEPOSIT);
            case BANK_WITHDRAW_CUSTOM -> handleBankCustom(player, text,
                    me.uc.hussein.ultrasclans.gui.holder.BankGuiHolder.Mode.WITHDRAW);
        }
    }

    private void handleCreateClanName(Player player, String text) {
        if (text.equalsIgnoreCase("cancel")) {
            plugin.getMessageService().send(player, "clan.create-cancelled");
            return;
        }

        plugin.getClanService().createClan(player, text, result -> {
            if (result.isSuccess()) {
                plugin.getMessageService().send(player, "clan.create-success",
                        Map.of("clan", result.getClan().getName()));
                plugin.playSound(player, "success");
                plugin.getGuiManager().openClanRoot(player);
                return;
            }

            String code = result.getFailCode();
            if (code.startsWith("COOLDOWN:")) {
                long seconds = Long.parseLong(code.substring("COOLDOWN:".length()));
                plugin.getMessageService().send(player, "clan.create-cooldown",
                        Map.of("time", me.uc.hussein.ultrasclans.util.TimeFormatter.format(seconds)));
            } else if (code.startsWith("NAME_INVALID:")) {
                String reason = code.substring("NAME_INVALID:".length());
                plugin.getMessageService().send(player, "clan.create-name-invalid",
                        Map.of("reason", reason));
            } else if (code.equals("NAME_TAKEN")) {
                plugin.getMessageService().send(player, "clan.create-name-taken");
            } else if (code.equals("NOT_ENOUGH_MONEY")) {
                double price = plugin.getConfigManager().get().getDouble("clan-creation.price", 1000.0);
                plugin.getMessageService().send(player, "clan.create-not-enough-money",
                        Map.of("price", plugin.getEconomyHook().format(price)));
            } else if (code.equals("ALREADY_IN_CLAN")) {
                plugin.getMessageService().send(player, "clan.already-in-clan");
            } else {
                plugin.getMessageService().send(player, "clan.create-failed-refunded");
            }
            plugin.playSound(player, "error");
        });
    }

    private void handleSearchClans(Player player, String text) {
        String query = text.equalsIgnoreCase("cancel") ? null : text;
        plugin.getGuiManager().clansList().open(player, 0, query);
    }

    private void handleRenameClan(Player player, String text) {
        // يُفعَّل بالكامل مع واجهة Settings في مرحلة قادمة.
        plugin.getMessageService().sendLiteral(player, "&7Clan renaming will be available with the Settings menu.");
    }

    private void handleBankCustom(Player player, String text,
                                   me.uc.hussein.ultrasclans.gui.holder.BankGuiHolder.Mode mode) {
        var clanOpt = plugin.getClanService().getPlayerClan(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            plugin.getMessageService().send(player, "clan.not-in-clan");
            return;
        }
        if (text.equalsIgnoreCase("cancel")) {
            plugin.getGuiManager().bank().open(player, clanOpt.get(), mode);
            return;
        }

        double amount = parseAmount(text);
        if (amount <= 0) {
            plugin.getMessageService().send(player, "bank.invalid-amount");
            plugin.playSound(player, "error");
            plugin.getGuiManager().bank().open(player, clanOpt.get(), mode);
            return;
        }
        plugin.getGuiManager().bankInteraction().execute(player, clanOpt.get(), mode, amount);
    }

    /**
     * يحلل مبلغًا مكتوبًا بأمان: يرفض النص الطويل، NaN، Infinity، السالب،
     * والقيم الضخمة جدًا (قسم 55: Negative amount / Integer overflow /
     * Long amount / NaN-Infinity). يُرجع -1 عند أي مشكلة.
     */
    private double parseAmount(String text) {
        if (text == null || text.length() > 15) {
            return -1;
        }
        try {
            double value = Double.parseDouble(text.replace(",", "").trim());
            if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0 || value > 1_000_000_000_000.0) {
                return -1;
            }
            return Math.floor(value * 100.0) / 100.0;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
