package dev.donuttpa.commands;

import dev.donuttpa.config.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /tpareload — Reloads all plugin config files. OP only.
 */
public class TpaReloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("donuttpa.tpareload")) {
            if (sender instanceof Player player) {
                ConfigManager.getInstance().sendMessage(player, "no-permission");
            } else {
                sender.sendMessage("No permission.");
            }
            return true;
        }

        ConfigManager.getInstance().loadAll();

        if (sender instanceof Player player) {
            ConfigManager.getInstance().sendMessage(player, "reload-success");
        } else {
            sender.sendMessage("[DonutTPA] Configs reloaded.");
        }

        return true;
    }
}
