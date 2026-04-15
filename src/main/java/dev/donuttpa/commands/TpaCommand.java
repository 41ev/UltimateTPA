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

import java.util.List;
import java.util.stream.Collectors;

/**
 * /tpa <player>
 *
 * Opens a send-confirm GUI showing info about the target.
 * The request is only dispatched when the sender clicks Confirm in the GUI.
 */
public class TpaCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("donuttpa.tpa")) {
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

        if (TpaManager.getInstance().isReceivingDisabled(target, RequestType.TPA)) {
            ConfigManager.getInstance().sendMessage(player, "tpa-disabled", "%player%", target.getName());
            SoundUtils.play(player, "error");
            return true;
        }

        // Open the send-confirm GUI — request is sent only when Confirm is clicked
        GuiManager.openSendGui(player, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
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
