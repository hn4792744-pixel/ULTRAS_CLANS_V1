package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.TasksGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanTaskProgress.Period;
import me.uc.hussein.ultrasclans.task.TaskService;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public final class TasksGui {

    public static final int SIZE = 27;
    public static final int SLOT_DAILY = 11;
    public static final int SLOT_WEEKLY = 13;
    public static final int SLOT_MONTHLY = 15;
    public static final int SLOT_BACK = 22;

    private final UltrasClansPlugin plugin;

    public TasksGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Clan clan) {
        TasksGuiHolder holder = new TasksGuiHolder(player.getUniqueId(), clan.getId());
        String title = plugin.getMessageService().text(player, "general.gui-title-tasks", Map.of("clan", clan.getName()));
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);
        holder.setInventory(inv);

        inv.setItem(SLOT_DAILY, buildTaskItem(clan, Period.DAILY, Material.CLOCK, "&e&lDaily Task"));
        inv.setItem(SLOT_WEEKLY, buildTaskItem(clan, Period.WEEKLY, Material.COMPASS, "&6&lWeekly Task"));
        inv.setItem(SLOT_MONTHLY, buildTaskItem(clan, Period.MONTHLY, Material.NETHER_STAR, "&d&lMonthly Task"));

        inv.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getMessageService().text(player, "general.back-button")).build());

        player.openInventory(inv);
    }

    private org.bukkit.inventory.ItemStack buildTaskItem(Clan clan, Period period, Material material, String title) {
        var view = plugin.getTaskService().view(clan, period);
        if (view.template() == null) {
            return new ItemBuilder(Material.BARRIER).name(title).lore("&7No task configured.").build();
        }

        int percent = view.percent();
        String bar = progressBar(percent);
        Material displayMaterial = view.claimed() ? Material.GRAY_DYE
                : (view.isComplete() ? Material.LIME_DYE : material);

        return new ItemBuilder(displayMaterial)
                .name(title + " &8- &f" + view.template().name())
                .lore(
                        "&7Progress: &f" + view.progress() + "/" + view.template().target(),
                        "&f" + bar + " &7" + percent + "%",
                        "&7Reward: &d" + view.template().rewardCp() + " CP",
                        "",
                        view.claimed() ? "&7Already claimed."
                                : (view.isComplete() ? "&aClick to claim!" : "&7Keep going!")
                ).build();
    }

    private String progressBar(int percent) {
        int filled = Math.max(0, Math.min(20, percent / 5));
        return "█".repeat(filled) + "░".repeat(20 - filled);
    }
}
