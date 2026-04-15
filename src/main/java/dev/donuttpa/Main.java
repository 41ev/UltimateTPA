package dev.donuttpa;

import dev.donuttpa.commands.*;
import dev.donuttpa.config.ConfigManager;
import dev.donuttpa.listeners.GuiClickListener;
import dev.donuttpa.listeners.PlayerListener;
import dev.donuttpa.storage.StorageManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * DonutTPA — Plugin entry point.
 * Initialises all managers, registers commands and event listeners.
 */
public final class Main extends JavaPlugin {

    private static Main instance;

    @Override
    public void onEnable() {
        instance = this;

        // Ensure data folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Load configs (copies defaults if missing)
        ConfigManager.getInstance().loadAll();

        // Initialise SQLite
        StorageManager.getInstance().init();

        // Register commands
        registerCommands();

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new GuiClickListener(), this);

        getLogger().info("DonutTPA enabled successfully!");
    }

    @Override
    public void onDisable() {
        StorageManager.getInstance().close();
        getLogger().info("DonutTPA disabled.");
    }

    public static Main getInstance() {
        return instance;
    }

    private void registerCommands() {
        // /tpa
        var tpaCmd = getCommand("tpa");
        if (tpaCmd != null) {
            TpaCommand tpaCommand = new TpaCommand();
            tpaCmd.setExecutor(tpaCommand);
            tpaCmd.setTabCompleter(tpaCommand);
        }

        // /tpahere
        var tpaHereCmd = getCommand("tpahere");
        if (tpaHereCmd != null) {
            TpaHereCommand tpaHereCommand = new TpaHereCommand();
            tpaHereCmd.setExecutor(tpaHereCommand);
            tpaHereCmd.setTabCompleter(tpaHereCommand);
        }

        // /tpaccept
        var tpAcceptCmd = getCommand("tpaccept");
        if (tpAcceptCmd != null) {
            TpAcceptCommand tpAcceptCommand = new TpAcceptCommand();
            tpAcceptCmd.setExecutor(tpAcceptCommand);
            tpAcceptCmd.setTabCompleter(tpAcceptCommand);
        }

        // /tpadeny
        var tpaDenyCmd = getCommand("tpadeny");
        if (tpaDenyCmd != null) {
            TpaDenyCommand tpaDenyCommand = new TpaDenyCommand();
            tpaDenyCmd.setExecutor(tpaDenyCommand);
            tpaDenyCmd.setTabCompleter(tpaDenyCommand);
        }

        // /tpacancel
        var tpaCancelCmd = getCommand("tpacancel");
        if (tpaCancelCmd != null) {
            tpaCancelCmd.setExecutor(new TpaCancelCommand());
        }

        // /tpatoggle
        var tpaToggleCmd = getCommand("tpatoggle");
        if (tpaToggleCmd != null) {
            tpaToggleCmd.setExecutor(new TpaToggleCommands.TpaToggle());
        }

        // /tpaheretoggle
        var tpaHereToggleCmd = getCommand("tpaheretoggle");
        if (tpaHereToggleCmd != null) {
            tpaHereToggleCmd.setExecutor(new TpaToggleCommands.TpaHereToggle());
        }

        // /tpaauto
        var tpaAutoCmd = getCommand("tpaauto");
        if (tpaAutoCmd != null) {
            tpaAutoCmd.setExecutor(new TpaToggleCommands.TpaAuto());
        }

        // /tpaguitoggle
        var tpaGuiToggleCmd = getCommand("tpaguitoggle");
        if (tpaGuiToggleCmd != null) {
            tpaGuiToggleCmd.setExecutor(new TpaToggleCommands.TpaGuiToggle());
        }

        // /tpareload
        var tpaReloadCmd = getCommand("tpareload");
        if (tpaReloadCmd != null) {
            tpaReloadCmd.setExecutor(new TpaReloadCommand());
        }

        // /tp
        var tpCmd = getCommand("tp");
        if (tpCmd != null) {
            TpCommand tpCommand = new TpCommand();
            tpCmd.setExecutor(tpCommand);
            tpCmd.setTabCompleter(tpCommand);
        }
    }
}
