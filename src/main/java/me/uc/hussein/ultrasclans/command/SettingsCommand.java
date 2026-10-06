package me.uc.hussein.ultrasclans.command;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /clansettings (أو /mysettings /ucprefs): يفتحها أي لاعب، بكلان أو بدونه. */
public final class SettingsCommand implements CommandExecutor {

    private final UltrasClansPlugin plugin;

    public SettingsCommand(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().send(sender, "general.player-only");
            return true;
        }
        plugin.getSettingsGui().open(player);
        return true;
    }
}
