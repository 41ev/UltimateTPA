package dev.donuttpa.commands;

import dev.donuttpa.config.ConfigManager;
import dev.donuttpa.utils.TeleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * /tp [player] [target] — Admin teleport.
 *   /tp <player>         — Teleport yourself to <player>
 *   /tp <p1> <p2>        — Teleport <p1> to <p2>
 */
public class TpCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("donuttpa.tp")) {
            if (sender instanceof Player player) {
                ConfigManager.getInstance().sendMessage(player, "no-permission");
            } else {
                sender.sendMessage("No permission.");
            }
            return true;
        }

        if (args.length == 1) {
            // /tp <target> — teleport self to target
            if (!(sender instanceof Player self)) {
                sender.sendMessage("Console must specify two player names.");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                ConfigManager.getInstance().sendMessage(self, "player-not-found");
                return true;
            }
            TeleportUtils.teleport(self, target.getLocation());
            self.sendMessage(TeleportUtils.colorize("&aTeleported to &e" + target.getName() + "&a."));
            return true;
        }

        if (args.length >= 2) {
            // /tp <p1> <p2>
            Player p1 = Bukkit.getPlayerExact(args[0]);
            Player p2 = Bukkit.getPlayerExact(args[1]);

            if (p1 == null || p2 == null) {
                if (sender instanceof Player player) {
                    ConfigManager.getInstance().sendMessage(player, "player-not-found");
                } else {
                    sender.sendMessage("One or both players not found.");
                }
                return true;
            }

            TeleportUtils.teleport(p1, p2.getLocation());
            sender.sendMessage(TeleportUtils.colorize("&aTeleported &e" + p1.getName()
                    + " &ato &e" + p2.getName() + "&a."));
            return true;
        }

        sender.sendMessage(command.getUsage());
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        String input = args.length > 0 ? args[args.length - 1].toLowerCase() : "";
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}
