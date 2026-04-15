package dev.donuttpa.utils;

import dev.donuttpa.config.ConfigManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Plays sounds defined in sounds.yml to players.
 */
public class SoundUtils {

    private SoundUtils() {}

    /**
     * Plays a sound key (from sounds.yml) to the given player.
     * @param player  The target player
     * @param key     The sounds.yml key, e.g. "count-down", "teleport-done"
     */
    public static void play(Player player, String key) {
        FileConfiguration sounds = ConfigManager.getInstance().getSoundsConfig();
        String path = "sounds." + key;

        boolean enabled = sounds.getBoolean(path + ".enabled", false);
        if (!enabled) return;

        String soundName = sounds.getString(path + ".sound", "BLOCK_NOTE_BLOCK_PLING");
        float volume = (float) sounds.getDouble(path + ".volume", 1.0);
        float pitch  = (float) sounds.getDouble(path + ".pitch", 1.0);

        // Convert ENUM_STYLE names to minecraft:snake_case for the registry lookup
        String normalized = soundName.toLowerCase().replace("_", ".");
        // Bukkit sound enum names use dots as namespace separators in the key,
        // e.g. BLOCK_NOTE_BLOCK_PLING → minecraft:block.note_block.pling
        // Simpler: just use the NamespacedKey from the enum constant as fallback
        Sound sound = resolveSound(soundName);
        if (sound == null) return;

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Resolves a sound name from config.
     * Accepts both Bukkit enum names (BLOCK_NOTE_BLOCK_PLING)
     * and namespaced keys (minecraft:block.note_block.pling).
     */
    private static Sound resolveSound(String soundName) {
        // Try Registry lookup with namespaced key first (new preferred API)
        try {
            String nsKey = soundName.toLowerCase();
            if (!nsKey.contains(":")) {
                // Convert Bukkit enum style: BLOCK_NOTE_BLOCK_PLING → block.note_block.pling
                nsKey = "minecraft:" + nsKey.replace("_", ".");
                // Re-apply: Bukkit enums use underscores between words, dots between categories
                // e.g. ENTITY_ENDERMAN_TELEPORT → entity.enderman.teleport
            }
            NamespacedKey namespacedKey = NamespacedKey.fromString(nsKey);
            if (namespacedKey != null) {
                Sound sound = Registry.SOUNDS.get(namespacedKey);
                if (sound != null) return sound;
            }
        } catch (Exception ignored) {}

        // Fallback: legacy enum lookup (suppressed deprecation — kept as safety net)
        try {
            //noinspection deprecation
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        return null;
    }
}
