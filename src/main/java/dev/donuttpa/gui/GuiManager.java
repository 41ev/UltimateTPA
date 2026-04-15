package dev.donuttpa.gui;

import dev.donuttpa.config.ConfigManager;
import dev.donuttpa.managers.TpaManager;
import dev.donuttpa.utils.TeleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds and opens TPA send/accept GUIs.
 */
public class GuiManager {

    // Metadata tags stored in item display names so click handlers can identify buttons
    public static final String TAG_ACCEPT  = "\u00a7rACCEPT";
    public static final String TAG_DENY    = "\u00a7rDENY";
    public static final String TAG_CONFIRM = "\u00a7rCONFIRM";
    public static final String TAG_CANCEL  = "\u00a7rCANCEL";

    private GuiManager() {}

    /**
     * Opens the TPA-send confirmation GUI (shown to the sender before sending).
     */
    public static void openSendGui(Player sender, Player target) {
        String rawTitle = ConfigManager.getInstance().getGuiSendConfig().getString("title", "&8TPA Request");
        Inventory inv = Bukkit.createInventory(null, 27, TeleportUtils.colorize(rawTitle));

        // Cancel button – slot 10
        inv.setItem(10, makeItem(Material.RED_STAINED_GLASS_PANE,
                TeleportUtils.colorize("&c&lCancel") + TAG_CANCEL,
                Arrays.asList(TeleportUtils.colorize("&7Click to cancel the request"))));

        // World icon – slot 12
        inv.setItem(12, makeItem(Material.GRASS_BLOCK,
                TeleportUtils.colorize("&aWorld: &e" + target.getWorld().getName()),
                Arrays.asList(TeleportUtils.colorize("&7Target's current world"))));

        // Player head – slot 13
        inv.setItem(13, makeHead(target,
                TeleportUtils.colorize("&e" + target.getName()),
                Arrays.asList(TeleportUtils.colorize("&7Send TPA request to this player"))));

        // Fly icon – slot 14
        String flyStatus = target.isFlying() ? "&aYes" : "&cNo";
        inv.setItem(14, makeItem(Material.FEATHER,
                TeleportUtils.colorize("&bFlying: " + flyStatus),
                Arrays.asList(TeleportUtils.colorize("&7Target's fly status"))));

        // Confirm button – slot 16
        inv.setItem(16, makeItem(Material.LIME_STAINED_GLASS_PANE,
                TeleportUtils.colorize("&a&lConfirm") + TAG_CONFIRM,
                Arrays.asList(TeleportUtils.colorize("&7Click to send the TPA request"))));

        sender.openInventory(inv);
    }

    /**
     * Opens the TPA-accept GUI (shown to the target when a request arrives).
     */
    public static void openAcceptGui(Player target, Player sender) {
        String rawTitle = ConfigManager.getInstance().getGuiAcceptConfig().getString("title", "&8TPA Request");
        Inventory inv = Bukkit.createInventory(null, 27, TeleportUtils.colorize(rawTitle));

        // Deny button – slot 10
        inv.setItem(10, makeItem(Material.RED_STAINED_GLASS_PANE,
                TeleportUtils.colorize("&c&lDeny") + TAG_DENY,
                Arrays.asList(TeleportUtils.colorize("&7Click to deny the request"))));

        // Player head – slot 13
        inv.setItem(13, makeHead(sender,
                TeleportUtils.colorize("&e" + sender.getName()),
                Arrays.asList(TeleportUtils.colorize("&7Wants to teleport to you"))));

        // Accept button – slot 16
        inv.setItem(16, makeItem(Material.LIME_STAINED_GLASS_PANE,
                TeleportUtils.colorize("&a&lAccept") + TAG_ACCEPT,
                Arrays.asList(TeleportUtils.colorize("&7Click to accept the request"))));

        target.openInventory(inv);
    }

    // ─── Item Factories ───────────────────────────────────────────────────────

    private static ItemStack makeItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(displayName);
        meta.setLore(new ArrayList<>(lore));
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    private static ItemStack makeHead(Player player, String displayName, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;
        meta.setOwningPlayer(player);
        meta.setDisplayName(displayName);
        meta.setLore(new ArrayList<>(lore));
        head.setItemMeta(meta);
        return head;
    }
}
