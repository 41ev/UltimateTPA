package dev.donuttpa.commands;

import dev.donuttpa.config.ConfigManager;
import dev.donuttpa.gui.GuiManager;
import dev.donuttpa.managers.TpaManager;
import dev.donuttpa.utils.SoundUtils;
import dev.donuttpa.utils.TpaRequest;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * /tpaccept [player]
 *
 * Opens the accept/deny GUI showing who sent the request.
 * The request is accepted or denied only when the target clicks a button in the GUI.
 */
public class TpAcceptCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("donuttpa.tpaccept")) {
            ConfigManager.getInstance().sendMessage(player, "no-permission");
            return true;
        }

        Set<TpaRequest> pending = TpaManager.getInstance().getPendingRequests(player.getUniqueId());
        if (pending.isEmpty()) {
            ConfigManager.getInstance().sendMessage(player, "no-pending-requests");
            SoundUtils.play(player, "error");
            return true;
        }

        // If a specific sender name is given, find that request; otherwise use the most recent
        TpaRequest toShow;
        if (args.length >= 1) {
            String senderName = args[0];
            toShow = pending.stream()
                    .filter(r -> {
                        Player p = Bukkit.getPlayer(r.getSender());
                        return p != null && p.getName().equalsIgnoreCase(senderName);
                    })
                    .findFirst()
                    .orElse(null);

            if (toShow == null) {
                ConfigManager.getInstance().sendMessage(player, "no-pending-requests");
                SoundUtils.play(player, "error");
                return true;
            }
        } else {
            toShow = pending.stream()
                    .max(Comparator.comparingLong(TpaRequest::getTimestamp))
                    .orElse(null);
        }

        if (toShow == null) return true;

        Player requestSender = Bukkit.getPlayer(toShow.getSender());
        if (requestSender == null || !requestSender.isOnline()) {
            ConfigManager.getInstance().sendMessage(player, "player-not-found");
            return true;
        }

        // Open the accept/deny GUI — actual accept/deny happens on button click
        GuiManager.openAcceptGui(player, requestSender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            String input = args[0].toLowerCase();
            return TpaManager.getInstance().getPendingRequestSenderNames(player).stream()
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .toList();
        }
        return List.of();
    }
}
