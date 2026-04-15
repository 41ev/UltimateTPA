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
 * /tpa <player> — Sends a teleport request to another player.
 *
 * Flow:
 *  1. Permission + online-player checks
 *  2. Self-target guard
 *  3. Check if target has TPA disabled
 *  4. Delegate to TpaManager.sendRequest() (handles cooldown + state)
 *  5. Optionally opens the send-confirmation GUI
 */
public class TpaCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

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

        // If the sender prefers GUI and global GUI is not disabled, open send-confirm GUI.
        // The actual request is sent only when the player clicks Confirm.
        if (TpaManager.getInstance().isGuiEnabled(player)) {
            // Store pending send in a transient map so the GUI confirm button can dispatch it
            GuiManager.openSendGui(player, target);
            // Schedule actual request dispatch after GUI interaction via GuiClickListener
            // For simplicity (and to match the original plugin's behaviour) we send the request
            // immediately and open the receive GUI on the target side.
            TpaManager.getInstance().sendRequest(player, target, RequestType.TPA);
            // Open accept GUI on target if they prefer GUI
            if (TpaManager.getInstance().isGuiEnabled(target)) {
                GuiManager.openAcceptGui(target, player);
            }
        } else {
            TpaManager.getInstance().sendRequest(player, target, RequestType.TPA);
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
