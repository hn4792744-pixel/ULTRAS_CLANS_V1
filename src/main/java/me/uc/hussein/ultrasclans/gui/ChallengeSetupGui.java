package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.event.LocationType;
import me.uc.hussein.ultrasclans.gui.holder.ChallengeSetupGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

public final class ChallengeSetupGui {

    public static final int SIZE = 27;
    public static final int SLOT_MONEY = 10;
    public static final int SLOT_CP = 11;
    public static final int SLOT_LOCATION = 12;
    public static final int SLOT_DURATION = 14;
    public static final int SLOT_ROUNDS = 15;
    public static final int SLOT_CONFIRM = 22;
    public static final int SLOT_BACK = 18;

    /** قيم جاهزة تتنقل بينها النقرة (قسم 33: Money 1/10/25/50/100/1000، CP اختيار). */
    public static final double[] MONEY_PRESETS = {0, 100, 500, 1000, 5000, 10000};
    public static final long[] CP_PRESETS = {0, 50, 100, 250, 500, 1000};

    private final UltrasClansPlugin plugin;

    public ChallengeSetupGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan, Clan target) {
        List<Integer> options = plugin.getConfigManager().loadYaml("events.yml").getIntegerList("round-duration-options-minutes");
        int defaultMinutes = options.isEmpty() ? 5 : options.get(0);
        ChallengeSetupGuiHolder holder = new ChallengeSetupGuiHolder(player.getUniqueId(), clan.getId(), target.getId(), defaultMinutes);
        render(player, holder, clan, target);
    }

    /** يُعيد رسم الواجهة بنفس الحالة الحالية (بعد كل نقرة تبديل). */
    public void reopen(Player player, ChallengeSetupGuiHolder holder) {
        var clanOpt = plugin.getClanService().getClan(holder.getClanId());
        var targetOpt = plugin.getClanService().getClan(holder.getTargetClanId());
        if (clanOpt.isEmpty() || targetOpt.isEmpty()) {
            player.closeInventory();
            return;
        }
        // نبني holder جديد بنفس القيم لأن Inventory مرتبط بـholder واحد فقط
        ChallengeSetupGuiHolder fresh = new ChallengeSetupGuiHolder(holder.getOwnerUuid(), holder.getClanId(),
                holder.getTargetClanId(), holder.getRoundDurationMinutes());
        fresh.setMoneyWager(holder.getMoneyWager());
        fresh.setCpWager(holder.getCpWager());
        fresh.setLocationType(holder.getLocationType());
        fresh.setTotalRounds(holder.getTotalRounds());
        render(player, fresh, clanOpt.get(), targetOpt.get());
    }

    private void render(Player player, ChallengeSetupGuiHolder holder, Clan clan, Clan target) {
        String title = plugin.getMessageService().text(player, "general.gui-title-challenge-setup", Map.of("clan", target.getName()));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        inv.setItem(SLOT_MONEY, new ItemBuilder(Material.GOLD_INGOT)
                .name("&e&lMoney Wager: &f" + plugin.getEconomyHook().format(holder.getMoneyWager()))
                .lore("&7Your bank: &f" + plugin.getEconomyHook().format(clan.getBankBalance()),
                        "", "&eClick to cycle amount.").build());

        inv.setItem(SLOT_CP, new ItemBuilder(Material.NETHER_STAR)
                .name("&d&lCP Wager: &f" + holder.getCpWager() + " CP")
                .lore("&7Your CP: &f" + clan.getCp(), "", "&eClick to cycle amount.").build());

        inv.setItem(SLOT_LOCATION, new ItemBuilder(Material.COMPASS)
                .name("&b&lBattle Location: &f" + describe(holder.getLocationType()))
                .lore("&7Click to cycle:", "&7Random Safe / Admin Arena / Clan Spawn").build());

        inv.setItem(SLOT_DURATION, new ItemBuilder(Material.CLOCK)
                .name("&6&lRound Duration: &f" + holder.getRoundDurationMinutes() + "m")
                .lore("&eClick to cycle.").build());

        inv.setItem(SLOT_ROUNDS, new ItemBuilder(Material.IRON_SWORD)
                .name("&c&lRounds: &f" + holder.getTotalRounds())
                .lore("&eClick to increase (wraps after the max).").build());

        boolean hasWager = holder.getMoneyWager() > 0 || holder.getCpWager() > 0;
        inv.setItem(SLOT_CONFIRM, new ItemBuilder(hasWager ? Material.LIME_WOOL : Material.GRAY_WOOL)
                .name(hasWager ? "&a&lSend Challenge" : "&7Set at least one wager first")
                .lore("&7Target: &f" + target.getName()).build());

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "general.back-button")).build());

        player.openInventory(inv);
    }

    private String describe(LocationType type) {
        return switch (type) {
            case RANDOM_SAFE -> "Random Safe Location";
            case ADMIN_ARENA -> "Admin Arena";
            case CLAN_SPAWN -> "Clan Spawn";
        };
    }
}
