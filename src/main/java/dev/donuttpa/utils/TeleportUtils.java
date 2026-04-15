package dev.donuttpa.utils;

import dev.donuttpa.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for color code translation and Folia-safe teleportation.
 */
public class TeleportUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static Boolean foliaDetected = null;

    private TeleportUtils() {}

    /**
     * Translates & color codes and &#RRGGBB hex codes.
     */
    public static String colorize(String input) {
        if (input == null) return "";
        // Hex color support
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("\u00A7x");
            for (char c : hex.toCharArray()) {
                replacement.append('\u00A7').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        // Standard & codes
        return sb.toString().replace('&', '\u00A7');
    }

    /**
     * Detects whether the server is running Folia.
     */
    public static boolean isFolia() {
        if (foliaDetected == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                foliaDetected = true;
            } catch (ClassNotFoundException e) {
                foliaDetected = false;
            }
        }
        return foliaDetected;
    }

    /**
     * Teleports a player to a location in a Folia-safe manner.
     */
    public static void teleport(Player player, Location location) {
        if (isFolia()) {
            // Folia: use entity scheduler
            try {
                player.getClass()
                        .getMethod("teleportAsync", Location.class)
                        .invoke(player, location);
            } catch (Exception e) {
                // Fall back to sync if reflection fails
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> player.teleport(location));
            }
        } else {
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> player.teleport(location));
        }
    }

    /**
     * Cancellable wrapper for Bukkit/Folia repeating tasks used in countdown timers.
     */
    public static class CancellableTask {

        private int taskId = -1;
        private boolean cancelled = false;

        public void setTaskId(int taskId) {
            this.taskId = taskId;
        }

        public void cancel() {
            cancelled = true;
            if (taskId != -1) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }
}
