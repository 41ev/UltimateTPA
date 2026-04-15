package dev.donuttpa.listeners;

import dev.donuttpa.managers.TpaManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player join/quit events — loads/clears TPA state.
 */
public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        TpaManager.getInstance().loadSettings(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        TpaManager.getInstance().handleQuit(event.getPlayer());
    }
}
