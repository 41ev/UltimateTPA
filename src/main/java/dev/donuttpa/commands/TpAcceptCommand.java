package dev.donuttpa.commands;

import dev.donuttpa.config.ConfigManager;
import dev.donuttpa.managers.TpaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /tpaccept [player] — Accept a pending TPA request.
 * If a player name is specified, accepts that specific request;
 * otherwise accepts the most recent one.
 */
public class TpAcceptCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("donuttpa.tpaccept")) {
            ConfigManager.getInstance().sendMessage(player, "no-permission");
            return true;
        }

        String senderName = args.length >= 1 ? args[0] : null;
        TpaManager.getInstance().acceptRequest(player, senderName);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            String input = args[0].toLowerCase();
            return TpaManager.getInstance().getPendingRequestSenderNames(player).stream()
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .toList();
        }
        return List.of();
    }
}
