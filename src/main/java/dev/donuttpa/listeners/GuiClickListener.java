package dev.donuttpa.listeners;

import dev.donuttpa.gui.GuiManager;
import dev.donuttpa.managers.TpaManager;
import dev.donuttpa.utils.SoundUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles inventory click events for TPA GUIs.
 * Identifies buttons by hidden tag suffixes appended to display names.
 */
public class GuiClickListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String name = meta.getDisplayName();

        // Accept button (target accepting a TPA request)
        if (name.endsWith(GuiManager.TAG_ACCEPT)) {
            event.setCancelled(true);
            player.closeInventory();
            // Accept most-recent pending request
            TpaManager.getInstance().acceptRequest(player, null);
            SoundUtils.play(player, "accept");
            return;
        }

        // Deny button
        if (name.endsWith(GuiManager.TAG_DENY)) {
            event.setCancelled(true);
            player.closeInventory();
            // Deny most-recent pending request — identify sender from pending list
            var senders = TpaManager.getInstance().getPendingRequestSenderNames(player);
            if (!senders.isEmpty()) {
                TpaManager.getInstance().denyRequest(player, senders.get(0));
            }
            SoundUtils.play(player, "decline");
            return;
        }

        // Confirm button (sender confirming they want to send)
        // The send-GUI title starts with the colourised prefix — we cancel all clicks in it
        // The confirm tag is embedded in the button name
        if (name.endsWith(GuiManager.TAG_CONFIRM)) {
            event.setCancelled(true);
            // The GUI stores no extra state; command4 already sent the request after GUI confirm
            // This button closes the GUI — actual request dispatch is handled in TPA command
            player.closeInventory();
            return;
        }

        // Cancel button in send GUI
        if (name.endsWith(GuiManager.TAG_CANCEL)) {
            event.setCancelled(true);
            player.closeInventory();
            TpaManager.getInstance().cancelRequests(player);
            SoundUtils.play(player, "cancel");
            return;
        }

        // Cancel all clicks inside our GUIs (prevent item theft)
        String title = event.getView().getTitle();
        if (title.contains("\u1d1b\u1d18\u1d00") || title.contains("TPA")) {
            event.setCancelled(true);
        }
    }
}
