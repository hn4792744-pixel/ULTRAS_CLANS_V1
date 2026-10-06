package me.uc.hussein.ultrasclans.command;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ClansCommand implements CommandExecutor {

    private final UltrasClansPlugin plugin;

    public ClansCommand(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().sendLiteral(sender, plugin.getMessageManager().get("general.player-only"));
            return true;
        }
        String search = args.length > 0 ? String.join(" ", args) : null;
        plugin.getGuiManager().clansList().open(player, 0, search);
        return true;
    }
}
