package me.uc.hussein.ultrasclans.hook;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * غلاف حول Vault Economy API. إذا لم يكن Vault أو أي مزود اقتصاد
 * مثبتًا، تُعطَّل كل العمليات التي تحتاج مال بأمان (بدلًا من رمي خطأ).
 */
public final class EconomyHook {

    private final UltrasClansPlugin plugin;
    private Economy economy;
    private boolean available;

    public EconomyHook(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        boolean enabledInConfig = plugin.getConfigManager().get().getBoolean("economy.enabled", true);
        if (!enabledInConfig) {
            available = false;
            return;
        }
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found - economy features (clan creation price, bank) are disabled.");
            available = false;
            return;
        }
        RegisteredServiceProvider<Economy> provider = plugin.getServer()
                .getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            plugin.getLogger().warning("No economy provider registered with Vault - economy features are disabled.");
            available = false;
            return;
        }
        this.economy = provider.getProvider();
        this.available = true;
    }

    public boolean isAvailable() {
        return available;
    }

    public double getBalance(OfflinePlayer player) {
        if (!available) {
            return 0;
        }
        return economy.getBalance(player);
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (!available) {
            return false;
        }
        return economy.has(player, amount);
    }

    /**
     * @return true إذا نجح الخصم. لا يخصم شيئًا إذا فشل (JDBC-like atomic behaviour من طرف Vault).
     */
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!available) {
            return false;
        }
        if (!economy.has(player, amount)) {
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!available) {
            return false;
        }
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        if (available) {
            return economy.format(amount);
        }
        String symbol = plugin.getConfigManager().get().getString("economy.currency-symbol", "$");
        return symbol + String.format("%,.2f", amount);
    }
}
