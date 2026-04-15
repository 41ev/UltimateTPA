package dev.donuttpa.listeners;

import dev.donuttpa.config.ConfigManager;
import dev.donuttpa.gui.GuiManager;
import dev.donuttpa.managers.TpaManager;
import dev.donuttpa.utils.RequestType;
import dev.donuttpa.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * Handles all GUI button clicks for TPA send/accept GUIs.
 * Button data is read from the item's PersistentDataContainer.
 */
public class GuiClickListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Only handle our GUIs — cancel all clicks to prevent item theft
        String title = event.getView().getTitle();
        if (!title.contains("TPA") && !title.contains("\u1d1b\u1d18\u1d00")) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        // Read action and player name from PDC — display name is untouched
        String action     = GuiManager.parseAction(meta);
        String playerName = GuiManager.parsePlayerName(meta);

        if (action == null) return;

        switch (action) {

            case GuiManager.ACTION_CONFIRM -> {
                // Sender confirmed — dispatch the TPA request
                player.closeInventory();
                UUID targetUUID = TpaManager.getInstance().consumePendingSendTarget(player.getUniqueId());
                if (targetUUID == null) return;
                Player target = Bukkit.getPlayer(targetUUID);
                if (target == null || !target.isOnline()) {
                    ConfigManager.getInstance().sendMessage(player, "player-not-found");
                    return;
                }
                TpaManager.getInstance().sendRequest(player, target, RequestType.TPA);
            }

            case GuiManager.ACTION_CANCEL -> {
                // Sender cancelled from the send GUI
                player.closeInventory();
                TpaManager.getInstance().consumePendingSendTarget(player.getUniqueId());
                SoundUtils.play(player, "cancel");
            }

            case GuiManager.ACTION_ACCEPT -> {
                // Target accepted from the accept GUI
                player.closeInventory();
                if (playerName == null) return;
                TpaManager.getInstance().acceptRequest(player, playerName);
            }

            case GuiManager.ACTION_DENY -> {
                // Target denied from the accept GUI
                player.closeInventory();
                if (playerName == null) return;
                TpaManager.getInstance().denyRequest(player, playerName);
            }
        }
    }
}
