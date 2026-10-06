package me.uc.hussein.ultrasclans.bank;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

/**
 * كل عملية هنا atomic على مستوى الكلان (synchronized) ولا يمكن أن
 * تتجاوز سعة البنك (قسم 20: "لا يحدث overflow"). التفاعل مع Vault
 * يتم دائمًا على الـmain thread قبل/بعد التعديل الذري على الكاش.
 */
public final class BankService {

    private final UltrasClansPlugin plugin;

    public BankService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    /** onResult يستقبل (النتيجة, المبلغ الفعلي المُنفَّذ). */
    public void deposit(Player player, Clan clan, double requestedAmount, BiConsumer<BankResult, Double> onResult) {
        if (requestedAmount <= 0) {
            onResult.accept(BankResult.INVALID_AMOUNT, 0.0);
            return;
        }
        boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_BANK_DEPOSIT)
                || plugin.getPermissionService().isOwner(player.getUniqueId());
        if (!allowed) {
            onResult.accept(BankResult.NO_PERMISSION, 0.0);
            return;
        }

        double space;
        synchronized (clan) {
            space = clan.getBankCapacity() - clan.getBankBalance();
        }
        if (space <= 0) {
            onResult.accept(BankResult.BANK_FULL, 0.0);
            return;
        }

        double actualAmount = Math.min(requestedAmount, space);

        if (!plugin.getEconomyHook().has(player, actualAmount)) {
            onResult.accept(BankResult.INSUFFICIENT_PLAYER_FUNDS, 0.0);
            return;
        }
        if (!plugin.getEconomyHook().withdraw(player, actualAmount)) {
            onResult.accept(BankResult.INSUFFICIENT_PLAYER_FUNDS, 0.0);
            return;
        }

        synchronized (clan) {
            clan.setBankBalance(clan.getBankBalance() + actualAmount);
        }
        plugin.getScheduler().runAsync(() -> plugin.getClanService().getClanRepository().update(clan));

        awardDepositCp(player, clan, actualAmount);
        plugin.getTaskService().incrementProgress(clan, me.uc.hussein.ultrasclans.task.TaskType.BANK_DEPOSIT, (long) actualAmount);

        boolean partial = actualAmount < requestedAmount;
        onResult.accept(partial ? BankResult.PARTIAL : BankResult.SUCCESS, actualAmount);
    }

    public void withdraw(Player player, Clan clan, double requestedAmount, BiConsumer<BankResult, Double> onResult) {
        if (requestedAmount <= 0) {
            onResult.accept(BankResult.INVALID_AMOUNT, 0.0);
            return;
        }
        boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_BANK_WITHDRAW)
                || plugin.getPermissionService().isOwner(player.getUniqueId());
        if (!allowed) {
            onResult.accept(BankResult.NO_PERMISSION, 0.0);
            return;
        }

        boolean success;
        synchronized (clan) {
            if (clan.getBankBalance() < requestedAmount) {
                success = false;
            } else {
                clan.setBankBalance(clan.getBankBalance() - requestedAmount);
                success = true;
            }
        }
        if (!success) {
            onResult.accept(BankResult.INSUFFICIENT_BANK_FUNDS, 0.0);
            return;
        }

        plugin.getScheduler().runAsync(() -> plugin.getClanService().getClanRepository().update(clan));
        plugin.getEconomyHook().deposit(player, requestedAmount);
        onResult.accept(BankResult.SUCCESS, requestedAmount);
    }

    private void awardDepositCp(Player player, Clan clan, double amountDeposited) {
        var config = plugin.getPointsService().getConfig();
        if (!config.getBoolean("bank-deposit.enabled", true)) {
            return;
        }
        double currencyPerCp = config.getDouble("bank-deposit.currency-per-cp", 1000.0);
        if (currencyPerCp <= 0) {
            return;
        }
        long cp = (long) (amountDeposited / currencyPerCp);
        long maxPerDeposit = config.getLong("bank-deposit.max-cp-per-deposit", 50);
        cp = Math.min(cp, maxPerDeposit);
        if (cp > 0) {
            plugin.getPointsService().addCp(player, clan, cp, "Bank Deposit");
        }
    }
}
