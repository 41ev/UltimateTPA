package dev.donuttpa.config;

import dev.donuttpa.Main;
import dev.donuttpa.utils.TeleportUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;

/**
 * Loads and caches all plugin config files.
 * Provides a unified message-sending API (chat / actionbar / title).
 */
public class ConfigManager {

    private static ConfigManager instance;

    private FileConfiguration settingsConfig;
    private FileConfiguration countdownConfig;
    private FileConfiguration cooldownConfig;
    private FileConfiguration soundsConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration guiSendConfig;
    private FileConfiguration guiAcceptConfig;

    private ConfigManager() {}

    public static ConfigManager getInstance() {
        if (instance == null) instance = new ConfigManager();
        return instance;
    }

    // ─── Load / Reload ────────────────────────────────────────────────────────

    public void loadAll() {
        Main plugin = Main.getInstance();

        // Copy defaults from jar if missing
        saveDefaultIfMissing(plugin, "basic-setup/settings.yml");
        saveDefaultIfMissing(plugin, "basic-setup/countdown.yml");
        saveDefaultIfMissing(plugin, "basic-setup/request-cooldown.yml");
        saveDefaultIfMissing(plugin, "basic-setup/sounds.yml");
        saveDefaultIfMissing(plugin, "basic-setup/messages.yml");
        saveDefaultIfMissing(plugin, "gui/tpa-send.yml");
        saveDefaultIfMissing(plugin, "gui/tpa-accept.yml");

        settingsConfig  = load(plugin, "basic-setup/settings.yml");
        countdownConfig = load(plugin, "basic-setup/countdown.yml");
        cooldownConfig  = load(plugin, "basic-setup/request-cooldown.yml");
        soundsConfig    = load(plugin, "basic-setup/sounds.yml");
        messagesConfig  = load(plugin, "basic-setup/messages.yml");
        guiSendConfig   = load(plugin, "gui/tpa-send.yml");
        guiAcceptConfig = load(plugin, "gui/tpa-accept.yml");
    }

    private void saveDefaultIfMissing(Main plugin, String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private FileConfiguration load(Main plugin, String path) {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), path));
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public FileConfiguration getSettingsConfig()  { return settingsConfig; }
    public FileConfiguration getCountdownConfig() { return countdownConfig; }
    public FileConfiguration getCooldownConfig()  { return cooldownConfig; }
    public FileConfiguration getSoundsConfig()    { return soundsConfig; }
    public FileConfiguration getMessagesConfig()  { return messagesConfig; }
    public FileConfiguration getGuiSendConfig()   { return guiSendConfig; }
    public FileConfiguration getGuiAcceptConfig() { return guiAcceptConfig; }

    public int getCountdown()      { return countdownConfig.getInt("countdown", 5); }
    public int getRequestCooldown(){ return cooldownConfig.getInt("request-cooldown", 1); }
    public boolean isGuiDisabled() { return settingsConfig.getBoolean("disable-all-guis", false); }
    public boolean isJustTpaccept(){ return settingsConfig.getBoolean("use-just-tpaccept", false); }

    // ─── Message Sending ──────────────────────────────────────────────────────

    /**
     * Sends a message bundle (chat + actionbar + title) to a player.
     * Supports placeholders: %player%, %player1%, %player2%, %time%, %sec%, %world%, %is_flying%
     */
    public void sendMessage(Player player, String key, String... placeholders) {
        FileConfiguration msg = messagesConfig;
        String path = "messages." + key;

        String prefix = TeleportUtils.colorize(msg.getString("messages.prefix", ""));

        // Chat
        boolean chatEnabled = msg.getBoolean(path + ".chat.enabled", false);
        if (chatEnabled) {
            List<String> lines = msg.getStringList(path + ".chat.message");
            for (String line : lines) {
                player.sendMessage(TeleportUtils.colorize(prefix + applyPlaceholders(line, placeholders)));
            }
        }

        // ActionBar
        boolean abEnabled = msg.getBoolean(path + ".actionbar.enabled", false);
        if (abEnabled) {
            String abMsg = msg.getString(path + ".actionbar.message", "");
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(TeleportUtils.colorize(applyPlaceholders(abMsg, placeholders)))
            );
        }

        // Title
        boolean titleEnabled = msg.getBoolean(path + ".title.enabled", false);
        if (titleEnabled) {
            String title    = TeleportUtils.colorize(applyPlaceholders(msg.getString(path + ".title.title", ""), placeholders));
            String subtitle = TeleportUtils.colorize(applyPlaceholders(msg.getString(path + ".title.subtitle", ""), placeholders));
            player.sendTitle(title, subtitle, 10, 70, 20);
        }
    }

    /** Applies key=value placeholder pairs. Pairs are: key0, value0, key1, value1... */
    private String applyPlaceholders(String text, String[] pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            text = text.replace(pairs[i], pairs[i + 1]);
        }
        return text;
    }
}
