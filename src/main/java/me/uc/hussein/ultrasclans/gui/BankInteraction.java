package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.bank.BankResult;
import me.uc.hussein.ultrasclans.gui.holder.BankGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * نقطة واحدة لتنفيذ عمليات البنك مع الرسائل والأصوات، حتى لا يتكرر
 * المنطق بين نقرات الـGUI (المبالغ الجاهزة) والمبلغ المخصص عبر الشات.
 */
public final class BankInteraction {

    private final UltrasClansPlugin plugin;

    public BankInteraction(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, Clan clan, BankGuiHolder.Mode mode, double amount) {
        var messages = plugin.getMessageManager();
        var economy = plugin.getEconomyHook();
        boolean deposit = mode == BankGuiHolder.Mode.DEPOSIT;

        BiConsumer<BankResult, Double> callback = (result, actual) -> {
            switch (result) {
                case SUCCESS -> {
                    plugin.getMessageService().send(player, deposit ? "bank.deposit-success" : "bank.withdraw-success",
                            Map.of("amount", economy.format(actual)));
                    plugin.playSound(player, deposit ? "bank_deposit" : "bank_withdraw");
                }
                case PARTIAL -> {
                    plugin.getMessageService().send(player, "bank.deposit-partial", Map.of("amount", economy.format(actual)));
                    plugin.playSound(player, "bank_deposit");
                }
                case INSUFFICIENT_PLAYER_FUNDS -> plugin.getMessageService().send(player, "bank.insufficient-player-funds");
                case INSUFFICIENT_BANK_FUNDS -> plugin.getMessageService().send(player, "bank.insufficient-bank-funds");
                case BANK_FULL -> plugin.getMessageService().send(player, "bank.bank-full");
                case INVALID_AMOUNT -> plugin.getMessageService().send(player, "bank.invalid-amount");
                case NO_PERMISSION -> plugin.getMessageService().send(player, deposit ? "bank.no-permission-deposit" : "bank.no-permission-withdraw");
            }
            plugin.getGuiManager().bank().open(player, clan, mode);
        };

        if (deposit) {
            plugin.getBankService().deposit(player, clan, amount, callback);
        } else {
            plugin.getBankService().withdraw(player, clan, amount, callback);
        }
    }
}
