package dev.donuttpa.managers;

import dev.donuttpa.Main;
import dev.donuttpa.config.ConfigManager;
import dev.donuttpa.storage.StorageManager;
import dev.donuttpa.utils.RequestType;
import dev.donuttpa.utils.SoundUtils;
import dev.donuttpa.utils.TeleportUtils;
import dev.donuttpa.utils.TeleportUtils.CancellableTask;
import dev.donuttpa.utils.TpaRequest;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core TPA state machine. Manages pending requests, countdowns, and player preferences.
 */
public class TpaManager {

    private static TpaManager instance;

    // UUID of target -> set of pending requests aimed at that target
    private final Map<UUID, Set<TpaRequest>> teleportRequests = new ConcurrentHashMap<>();

    // "senderUUID-targetUUID" -> last request timestamp (millis)
    private final Map<String, Long> lastRequestTimes = new ConcurrentHashMap<>();

    // UUID of the teleporting player -> active countdown task
    private final Map<UUID, CancellableTask> teleportTasks = new ConcurrentHashMap<>();

    // Per-player preference sets (loaded from DB on join)
    private final Set<UUID> tpaDisabled     = ConcurrentHashMap.newKeySet();
    private final Set<UUID> tpaHereDisabled = ConcurrentHashMap.newKeySet();
    private final Set<UUID> tpaAutoEnabled  = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Boolean> guiPreferences = new ConcurrentHashMap<>();

    // Stores who a sender is targeting while the send-confirm GUI is open
    private final Map<UUID, UUID> pendingSendGui = new ConcurrentHashMap<>();

    private TpaManager() {}

    public static TpaManager getInstance() {
        if (instance == null) instance = new TpaManager();
        return instance;
    }

    // ─── Pending Send GUI ─────────────────────────────────────────────────────

    public void setPendingSendTarget(UUID sender, UUID target) {
        pendingSendGui.put(sender, target);
    }

    /** Returns and removes the pending target UUID for a sender. */
    public UUID consumePendingSendTarget(UUID sender) {
        return pendingSendGui.remove(sender);
    }

    // ─── Settings Persistence ─────────────────────────────────────────────────

    public void loadSettings(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            StorageManager.SettingsResult settings = StorageManager.getInstance().loadSettings(player.getUniqueId());
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                UUID id = player.getUniqueId();
                if (settings.tpaDisabled())     tpaDisabled.add(id);     else tpaDisabled.remove(id);
                if (settings.tpaHereDisabled()) tpaHereDisabled.add(id); else tpaHereDisabled.remove(id);
                if (settings.autoAccept())      tpaAutoEnabled.add(id);  else tpaAutoEnabled.remove(id);
                guiPreferences.put(id, settings.guiEnabled());
            });
        });
    }

    private void saveSettings(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () ->
            StorageManager.getInstance().saveSettings(
                    uuid,
                    tpaDisabled.contains(uuid),
                    tpaHereDisabled.contains(uuid),
                    guiPreferences.getOrDefault(uuid, true),
                    tpaAutoEnabled.contains(uuid)
            )
        );
    }

    public void handleQuit(Player player) {
        UUID id = player.getUniqueId();
        CancellableTask task = teleportTasks.remove(id);
        if (task != null) task.cancel();
        teleportRequests.remove(id);
        pendingSendGui.remove(id);
        guiPreferences.remove(id);
    }

    // ─── Request Management ───────────────────────────────────────────────────

    public boolean sendRequest(Player sender, Player target, RequestType type) {
        String cooldownKey = sender.getUniqueId() + "-" + target.getUniqueId();
        int cooldownSeconds = ConfigManager.getInstance().getRequestCooldown();

        Long lastTime = lastRequestTimes.get(cooldownKey);
        if (lastTime != null) {
            long elapsed = (System.currentTimeMillis() - lastTime) / 1000L;
            if (elapsed < cooldownSeconds) {
                long remaining = cooldownSeconds - elapsed;
                ConfigManager.getInstance().sendMessage(sender, "on-cooldown",
                        "%time%", formatCooldown(remaining));
                SoundUtils.play(sender, "error");
                return false;
            }
        }

        lastRequestTimes.put(cooldownKey, System.currentTimeMillis());

        TpaRequest request = new TpaRequest(sender.getUniqueId(), target.getUniqueId(), type);
        teleportRequests.computeIfAbsent(target.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(request);

        // Notify sender
        ConfigManager.getInstance().sendMessage(sender, "sent-request", "%player%", target.getName());
        SoundUtils.play(sender, "send");

        // Notify target
        String msgKey = (type == RequestType.TPAHERE) ? "receive-here-request" : "receive-request";
        ConfigManager.getInstance().sendMessage(target, msgKey, "%player%", sender.getName());
        SoundUtils.play(target, "accept");

        // Auto-accept
        if (tpaAutoEnabled.contains(target.getUniqueId())) {
            acceptRequest(target, sender.getName());
        }

        return true;
    }

    public void acceptRequest(Player target, String senderName) {
        Set<TpaRequest> pending = teleportRequests.get(target.getUniqueId());
        if (pending == null || pending.isEmpty()) {
            ConfigManager.getInstance().sendMessage(target, "no-pending-requests");
            SoundUtils.play(target, "error");
            return;
        }

        TpaRequest toAccept;

        if (senderName == null || senderName.isBlank()) {
            toAccept = pending.stream()
                    .max(Comparator.comparingLong(TpaRequest::getTimestamp))
                    .orElse(null);
        } else {
            Player senderPlayer = Bukkit.getPlayerExact(senderName);
            if (senderPlayer == null) {
                ConfigManager.getInstance().sendMessage(target, "player-not-found");
                SoundUtils.play(target, "error");
                return;
            }
            final UUID senderUUID = senderPlayer.getUniqueId();
            toAccept = pending.stream()
                    .filter(r -> r.getSender().equals(senderUUID))
                    .findFirst()
                    .orElse(null);
        }

        if (toAccept == null) {
            ConfigManager.getInstance().sendMessage(target, "no-pending-requests");
            SoundUtils.play(target, "error");
            return;
        }

        Player sender = Bukkit.getPlayer(toAccept.getSender());
        if (sender == null || !sender.isOnline()) {
            pending.remove(toAccept);
            ConfigManager.getInstance().sendMessage(target, "player-not-found");
            return;
        }

        pending.remove(toAccept);

        int countdownSeconds = ConfigManager.getInstance().getCountdown();

        final Player teleportingPlayer = (toAccept.getType() == RequestType.TPA) ? sender : target;
        final Player destinationPlayer = (toAccept.getType() == RequestType.TPA) ? target : sender;

        ConfigManager.getInstance().sendMessage(target, "you-accepted", "%player%", sender.getName());
        ConfigManager.getInstance().sendMessage(sender, "request-accepted",
                "%player%", target.getName(), "%time%", String.valueOf(countdownSeconds));
        SoundUtils.play(target, "accept");

        Location startLocation = teleportingPlayer.getLocation().clone();
        CancellableTask countdownTask = new CancellableTask();
        final int[] secondsLeft = {countdownSeconds};

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getInstance(), () -> {
            if (countdownTask.isCancelled()) return;

            // Movement check
            if (startLocation.getWorld() != null &&
                startLocation.getWorld().equals(teleportingPlayer.getWorld()) &&
                teleportingPlayer.getLocation().distanceSquared(startLocation) > 0.01) {
                countdownTask.cancel();
                teleportTasks.remove(teleportingPlayer.getUniqueId());
                ConfigManager.getInstance().sendMessage(teleportingPlayer, "moved-cancelled");
                SoundUtils.play(teleportingPlayer, "cancel");
                return;
            }

            if (secondsLeft[0] <= 0) {
                countdownTask.cancel();
                teleportTasks.remove(teleportingPlayer.getUniqueId());
                TeleportUtils.teleport(teleportingPlayer, destinationPlayer.getLocation());
                ConfigManager.getInstance().sendMessage(teleportingPlayer, "teleport-done");
                SoundUtils.play(teleportingPlayer, "teleport-done");
                SoundUtils.play(destinationPlayer, "teleport-done");
                return;
            }

            ConfigManager.getInstance().sendMessage(teleportingPlayer, "countdown",
                    "%sec%", String.valueOf(secondsLeft[0]));
            SoundUtils.play(teleportingPlayer, "count-down");
            secondsLeft[0]--;

        }, 0L, 20L);

        countdownTask.setTaskId(taskId);
        teleportTasks.put(teleportingPlayer.getUniqueId(), countdownTask);
    }

    public void denyRequest(Player target, String senderName) {
        Set<TpaRequest> pending = teleportRequests.get(target.getUniqueId());
        if (pending == null || pending.isEmpty()) {
            ConfigManager.getInstance().sendMessage(target, "no-pending-requests");
            SoundUtils.play(target, "error");
            return;
        }

        Player senderPlayer = Bukkit.getPlayerExact(senderName);
        if (senderPlayer == null) {
            ConfigManager.getInstance().sendMessage(target, "player-not-found");
            SoundUtils.play(target, "error");
            return;
        }

        final UUID senderUUID = senderPlayer.getUniqueId();
        TpaRequest toDeny = pending.stream()
                .filter(r -> r.getSender().equals(senderUUID))
                .findFirst()
                .orElse(null);

        if (toDeny == null) {
            ConfigManager.getInstance().sendMessage(target, "no-pending-requests");
            SoundUtils.play(target, "error");
            return;
        }

        pending.remove(toDeny);
        ConfigManager.getInstance().sendMessage(target, "you-denied", "%player%", senderPlayer.getName());
        ConfigManager.getInstance().sendMessage(senderPlayer, "request-denied", "%player%", target.getName());
        SoundUtils.play(target, "decline");
        SoundUtils.play(senderPlayer, "decline");
    }

    public void cancelRequests(Player sender) {
        UUID senderUUID = sender.getUniqueId();
        boolean found = false;
        for (Set<TpaRequest> requests : teleportRequests.values()) {
            found |= requests.removeIf(r -> r.getSender().equals(senderUUID));
        }
        pendingSendGui.remove(senderUUID);
        if (found) {
            ConfigManager.getInstance().sendMessage(sender, "request-cancelled");
            SoundUtils.play(sender, "cancel");
        } else {
            ConfigManager.getInstance().sendMessage(sender, "no-pending-requests");
            SoundUtils.play(sender, "error");
        }
    }

    // ─── Toggles ──────────────────────────────────────────────────────────────

    public void toggleTpa(Player player) {
        UUID id = player.getUniqueId();
        if (tpaDisabled.contains(id)) {
            tpaDisabled.remove(id);
            ConfigManager.getInstance().sendMessage(player, "tpa-toggle-on");
        } else {
            tpaDisabled.add(id);
            ConfigManager.getInstance().sendMessage(player, "tpa-toggle-off");
        }
        saveSettings(id);
    }

    public void toggleTpaHere(Player player) {
        UUID id = player.getUniqueId();
        if (tpaHereDisabled.contains(id)) {
            tpaHereDisabled.remove(id);
            ConfigManager.getInstance().sendMessage(player, "tpahere-toggle-on");
        } else {
            tpaHereDisabled.add(id);
            ConfigManager.getInstance().sendMessage(player, "tpahere-toggle-off");
        }
        saveSettings(id);
    }

    public void toggleTpaAuto(Player player) {
        UUID id = player.getUniqueId();
        if (tpaAutoEnabled.contains(id)) {
            tpaAutoEnabled.remove(id);
            ConfigManager.getInstance().sendMessage(player, "auto-toggle-off");
        } else {
            tpaAutoEnabled.add(id);
            ConfigManager.getInstance().sendMessage(player, "auto-toggle-on");
        }
        saveSettings(id);
    }

    public void toggleGui(Player player) {
        UUID id = player.getUniqueId();
        boolean current = guiPreferences.getOrDefault(id, true);
        guiPreferences.put(id, !current);
        ConfigManager.getInstance().sendMessage(player, current ? "gui-toggle-off" : "gui-toggle-on");
        saveSettings(id);
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    public boolean isReceivingDisabled(Player player, RequestType type) {
        return (type == RequestType.TPA)
                ? tpaDisabled.contains(player.getUniqueId())
                : tpaHereDisabled.contains(player.getUniqueId());
    }

    public Set<TpaRequest> getPendingRequests(UUID targetUUID) {
        return teleportRequests.getOrDefault(targetUUID, Collections.emptySet());
    }

    public List<String> getPendingRequestSenderNames(Player target) {
        List<String> names = new ArrayList<>();
        for (TpaRequest req : getPendingRequests(target.getUniqueId())) {
            Player p = Bukkit.getPlayer(req.getSender());
            if (p != null) names.add(p.getName());
        }
        return names;
    }

    public String formatCooldown(long seconds) {
        if (seconds < 60) return seconds + "s";
        return (seconds / 60) + "m " + (seconds % 60) + "s";
    }
}
