package dev.donuttpa.storage;

import dev.donuttpa.Main;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SQLite-backed persistence layer for per-player settings.
 */
public class StorageManager {

    private static StorageManager instance;
    private Connection connection;

    private StorageManager() {}

    public static StorageManager getInstance() {
        if (instance == null) instance = new StorageManager();
        return instance;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    public void init() {
        try {
            File dbFile = new File(Main.getInstance().getDataFolder(), "data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTable();
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "Failed to initialise SQLite database", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
    }

    private void ensureConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                init();
            }
        } catch (SQLException e) {
            init();
        }
    }

    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_settings (
                    uuid            TEXT PRIMARY KEY,
                    tpa_disabled    INTEGER NOT NULL DEFAULT 0,
                    tpahere_disabled INTEGER NOT NULL DEFAULT 0,
                    gui_enabled     INTEGER NOT NULL DEFAULT 1,
                    auto_accept     INTEGER NOT NULL DEFAULT 0
                )
                """);
        }
    }

    // ─── DTO ──────────────────────────────────────────────────────────────────

    public record SettingsResult(
            boolean tpaDisabled,
            boolean tpaHereDisabled,
            boolean guiEnabled,
            boolean autoAccept
    ) {}

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * Loads settings for a player, returning defaults if not yet stored.
     */
    public SettingsResult loadSettings(UUID uuid) {
        ensureConnection();
        String sql = "SELECT tpa_disabled, tpahere_disabled, gui_enabled, auto_accept FROM player_settings WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new SettingsResult(
                        rs.getInt("tpa_disabled") == 1,
                        rs.getInt("tpahere_disabled") == 1,
                        rs.getInt("gui_enabled") == 1,
                        rs.getInt("auto_accept") == 1
                );
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.WARNING, "Failed to load settings for " + uuid, e);
        }
        return new SettingsResult(false, false, true, false);
    }

    /**
     * Upserts all settings for a player.
     */
    public void saveSettings(UUID uuid, boolean tpaDisabled, boolean tpaHereDisabled, boolean guiEnabled, boolean autoAccept) {
        ensureConnection();
        String sql = """
            INSERT INTO player_settings (uuid, tpa_disabled, tpahere_disabled, gui_enabled, auto_accept)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                tpa_disabled     = excluded.tpa_disabled,
                tpahere_disabled = excluded.tpahere_disabled,
                gui_enabled      = excluded.gui_enabled,
                auto_accept      = excluded.auto_accept
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, tpaDisabled ? 1 : 0);
            ps.setInt(3, tpaHereDisabled ? 1 : 0);
            ps.setInt(4, guiEnabled ? 1 : 0);
            ps.setInt(5, autoAccept ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.WARNING, "Failed to save settings for " + uuid, e);
        }
    }
}
