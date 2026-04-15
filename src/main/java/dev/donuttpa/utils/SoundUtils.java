package dev.donuttpa.utils;

import dev.donuttpa.config.ConfigManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SoundUtils {

    private SoundUtils() {}

    public static void play(Player player, String key) {
        FileConfiguration sounds = ConfigManager.getInstance().getSoundsConfig();
        String path = "sounds." + key;

        boolean enabled = sounds.getBoolean(path + ".enabled", false);
        if (!enabled) return;

        String soundName = sounds.getString(path + ".sound", "BLOCK_NOTE_BLOCK_PLING");
        float volume = (float) sounds.getDouble(path + ".volume", 1.0);
        float pitch  = (float) sounds.getDouble(path + ".pitch", 1.0);

        Sound sound = resolveSound(soundName);
        if (sound == null) return;

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private static Sound resolveSound(String soundName) {
        if (soundName == null || soundName.isBlank()) return null;

        String nsKeyStr = soundName.toLowerCase();
        if (!nsKeyStr.contains(":")) {
            nsKeyStr = "minecraft:" + nsKeyStr.replace("_", ".");
        }

        NamespacedKey namespacedKey = NamespacedKey.fromString(nsKeyStr);
        if (namespacedKey == null) return null;

        return Registry.SOUNDS.get(namespacedKey);
    }
}