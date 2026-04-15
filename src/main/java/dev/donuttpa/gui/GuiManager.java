package dev.donuttpa.gui;

import dev.donuttpa.Main;
import dev.donuttpa.config.ConfigManager;
import dev.donuttpa.managers.TpaManager;
import dev.donuttpa.utils.TeleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds and opens TPA send/accept GUIs.
 *
 * Button data (action + player name) is stored invisibly in the item's
 * PersistentDataContainer so nothing leaks into the visible display name.
 */
public class GuiManager {

    // PDC keys
    public static final NamespacedKey KEY_ACTION = new NamespacedKey(Main.getInstance(), "tpa_action");
    public static final NamespacedKey KEY_PLAYER  = new NamespacedKey(Main.getInstance(), "tpa_player");

    // Action constants
    public static final String ACTION_CONFIRM = "CONFIRM";
    public static final String ACTION_CANCEL  = "CANCEL";
    public static final String ACTION_ACCEPT  = "ACCEPT";
    public static final String ACTION_DENY    = "DENY";

    private GuiManager() {}

    // ─── Send GUI ─────────────────────────────────────────────────────────────

    /**
     * Opens the send-confirm GUI for the player who typed /tpa <target>.
     * Clicking Confirm dispatches the request; Cancel closes without sending.
     */
    public static void openSendGui(Player sender, Player target) {
        TpaManager.getInstance().setPendingSendTarget(sender.getUniqueId(), target.getUniqueId());

        String rawTitle = ConfigManager.getInstance().getGuiSendConfig()
                .getString("title", "&8TPA Request");
        Inventory inv = Bukkit.createInventory(null, 27, TeleportUtils.colorize(rawTitle));

        inv.setItem(10, makeButton(Material.RED_STAINED_GLASS_PANE,
                "&c&lCancel", "&7Click to cancel",
                ACTION_CANCEL, target.getName()));

        inv.setItem(12, makeInfo(Material.GRASS_BLOCK,
                "&aWorld: &e" + target.getWorld().getName(),
                "&7Target's current world"));

        inv.setItem(13, makeHead(target,
                "&e" + target.getName(),
                "&7Send a TPA request to this player"));

        String flyStatus = target.isFlying() ? "&aYes" : "&cNo";
        inv.setItem(14, makeInfo(Material.FEATHER,
                "&bFlying: " + flyStatus,
                "&7Target's fly status"));

        inv.setItem(16, makeButton(Material.LIME_STAINED_GLASS_PANE,
                "&a&lConfirm", "&7Click to send the request",
                ACTION_CONFIRM, target.getName()));

        sender.openInventory(inv);
    }

    // ─── Accept GUI ───────────────────────────────────────────────────────────

    /**
     * Opens the accept/deny GUI shown to the target when they run /tpaccept.
     */
    public static void openAcceptGui(Player target, Player sender) {
        String rawTitle = ConfigManager.getInstance().getGuiAcceptConfig()
                .getString("title", "&8TPA Request");
        Inventory inv = Bukkit.createInventory(null, 27, TeleportUtils.colorize(rawTitle));

        inv.setItem(10, makeButton(Material.RED_STAINED_GLASS_PANE,
                "&c&lDeny", "&7Click to deny the request",
                ACTION_DENY, sender.getName()));

        inv.setItem(13, makeHead(sender,
                "&e" + sender.getName(),
                "&7Wants to teleport to you"));

        inv.setItem(16, makeButton(Material.LIME_STAINED_GLASS_PANE,
                "&a&lAccept", "&7Click to accept the request",
                ACTION_ACCEPT, sender.getName()));

        target.openInventory(inv);
    }

    // ─── Item Factories ───────────────────────────────────────────────────────

    /**
     * Creates a button item. Action and player name are stored in the PDC —
     * the display name is clean with no hidden data appended.
     */
    private static ItemStack makeButton(Material material, String label, String loreLine,
                                        String action, String playerName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Clean display name — no hidden tags
        meta.setDisplayName(TeleportUtils.colorize(label));
        List<String> lore = new ArrayList<>();
        lore.add(TeleportUtils.colorize(loreLine));
        meta.setLore(lore);

        // Store action and player name invisibly in the PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_ACTION, PersistentDataType.STRING, action);
        pdc.set(KEY_PLAYER,  PersistentDataType.STRING, playerName);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeInfo(Material material, String label, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(TeleportUtils.colorize(label));
        List<String> lore = new ArrayList<>();
        lore.add(TeleportUtils.colorize(loreLine));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    private static ItemStack makeHead(Player player, String label, String loreLine) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;
        meta.setOwningPlayer(player);
        meta.setDisplayName(TeleportUtils.colorize(label));
        List<String> lore = new ArrayList<>();
        lore.add(TeleportUtils.colorize(loreLine));
        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    // ─── Click Parsing ────────────────────────────────────────────────────────

    /** Returns the action stored in an item's PDC, or null if not a button. */
    public static String parseAction(ItemMeta meta) {
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(KEY_ACTION, PersistentDataType.STRING);
    }

    /** Returns the player name stored in an item's PDC, or null if absent. */
    public static String parsePlayerName(ItemMeta meta) {
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(KEY_PLAYER, PersistentDataType.STRING);
    }
}
