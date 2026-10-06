package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.BankGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public final class BankGui {

    public static final int SIZE = 45;
    public static final int SLOT_INFO = 4;
    public static final int SLOT_MODE_DEPOSIT = 20;
    public static final int SLOT_MODE_WITHDRAW = 24;
    public static final int[] AMOUNT_SLOTS = {28, 29, 30, 31, 32, 33, 34};
    public static final double[] PRESET_AMOUNTS = {1, 10, 25, 50, 100, 1000, -1}; // -1 = مبلغ مخصص
    public static final int SLOT_BACK = 40;

    private final UltrasClansPlugin plugin;

    public BankGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan, BankGuiHolder.Mode mode) {
        BankGuiHolder holder = new BankGuiHolder(player.getUniqueId(), clan.getId(), mode);
        String title = plugin.getMessageService().text(player, "general.gui-title-bank", Map.of("clan", clan.getName()));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        inv.setItem(SLOT_INFO, new ItemBuilder(Material.GOLD_INGOT)
                .name("&e&lClan Bank")
                .lore(
                        "&7Balance: &f" + plugin.getEconomyHook().format(clan.getBankBalance()),
                        "&7Capacity: &f" + plugin.getEconomyHook().format(clan.getBankCapacity()),
                        "&7Free space: &f" + plugin.getEconomyHook().format(clan.getBankCapacity() - clan.getBankBalance())
                ).build());

        boolean deposit = mode == BankGuiHolder.Mode.DEPOSIT;
        inv.setItem(SLOT_MODE_DEPOSIT, new ItemBuilder(deposit ? Material.LIME_STAINED_GLASS_PANE : Material.GREEN_STAINED_GLASS_PANE)
                .name(deposit ? "&a&lDeposit Mode (Active)" : "&2Deposit Mode")
                .lore("&7Click to switch to depositing.").build());
        inv.setItem(SLOT_MODE_WITHDRAW, new ItemBuilder(!deposit ? Material.RED_STAINED_GLASS_PANE : Material.PINK_STAINED_GLASS_PANE)
                .name(!deposit ? "&c&lWithdraw Mode (Active)" : "&4Withdraw Mode")
                .lore("&7Click to switch to withdrawing.").build());

        for (int i = 0; i < AMOUNT_SLOTS.length; i++) {
            double amount = PRESET_AMOUNTS[i];
            boolean custom = amount < 0;
            inv.setItem(AMOUNT_SLOTS[i], new ItemBuilder(custom ? Material.PAPER : Material.SUNFLOWER)
                    .name(custom ? "&e&lCustom Amount" : "&f" + plugin.getEconomyHook().format(amount))
                    .lore(custom ? "&7Type an amount in chat." : "&7Click to " + (deposit ? "deposit" : "withdraw") + ".")
                    .build());
        }

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "general.back-button")).build());

        player.openInventory(inv);
    }
}
