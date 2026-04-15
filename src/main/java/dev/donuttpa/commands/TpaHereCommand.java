package dev.donuttpa.commands;

import dev.donuttpa.config.ConfigManager;
import dev.donuttpa.gui.GuiManager;
import dev.donuttpa.managers.TpaManager;
import dev.donuttpa.utils.RequestType;
import dev.donuttpa.utils.SoundUtils;
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
 * /tpahere <player> — Requests another player to teleport to you.
 */
public class TpaHereCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("donuttpa.tpahere")) {
            ConfigManager.getInstance().sendMessage(player, "no-permission");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(command.getUsage());
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            ConfigManager.getInstance().sendMessage(player, "player-not-found");
            SoundUtils.play(player, "error");
            return true;
        }

        if (target.equals(player)) {
            ConfigManager.getInstance().sendMessage(player, "no-self-tpa");
            SoundUtils.play(player, "error");
            return true;
        }

        if (TpaManager.getInstance().isReceivingDisabled(target, RequestType.TPAHERE)) {
            ConfigManager.getInstance().sendMessage(player, "tpahere-disabled", "%player%", target.getName());
            SoundUtils.play(player, "error");
            return true;
        }

        TpaManager.getInstance().sendRequest(player, target, RequestType.TPAHERE);

        // Open accept GUI on the target side if they prefer it
        if (TpaManager.getInstance().isGuiEnabled(target)) {
            GuiManager.openAcceptGui(target, player);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
