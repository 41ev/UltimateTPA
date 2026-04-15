package dev.donuttpa.commands;

import dev.donuttpa.config.ConfigManager;
import dev.donuttpa.managers.TpaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Shared executor for all toggle commands:
 *   /tpatoggle, /tpaheretoggle, /tpaauto (/tpauto), /tpaguitoggle
 */
public class TpaToggleCommands {

    /** /tpatoggle */
    public static class TpaToggle implements CommandExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                 @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (!player.hasPermission("donuttpa.tpatoggle")) {
                ConfigManager.getInstance().sendMessage(player, "no-permission");
                return true;
            }
            TpaManager.getInstance().toggleTpa(player);
            return true;
        }
    }

    /** /tpaheretoggle */
    public static class TpaHereToggle implements CommandExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                 @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (!player.hasPermission("donuttpa.tpaheretoggle")) {
                ConfigManager.getInstance().sendMessage(player, "no-permission");
                return true;
            }
            TpaManager.getInstance().toggleTpaHere(player);
            return true;
        }
    }

    /** /tpaauto (/tpauto) */
    public static class TpaAuto implements CommandExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                 @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (!player.hasPermission("donuttpa.tpaauto")) {
                ConfigManager.getInstance().sendMessage(player, "no-permission");
                return true;
            }
            TpaManager.getInstance().toggleTpaAuto(player);
            return true;
        }
    }

    /** /tpaguitoggle */
    public static class TpaGuiToggle implements CommandExecutor {
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                 @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (!player.hasPermission("donuttpa.tpaguitoggle")) {
                ConfigManager.getInstance().sendMessage(player, "no-permission");
                return true;
            }
            TpaManager.getInstance().toggleGui(player);
            return true;
        }
    }
}
