package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class PlayerSettingsRepository {

    public record Settings(boolean clanChat, boolean tagEnabled) {
        public static final Settings DEFAULT = new Settings(false, true);
    }

    private final DatabaseManager db;

    public PlayerSettingsRepository(DatabaseManager db) {
        this.db = db;
    }

    public Settings load(UUID uuid) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT clan_chat, tag_enabled FROM uc_player_settings WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Settings(rs.getInt("clan_chat") == 1, rs.getInt("tag_enabled") == 1);
                }
            }
        }
        return Settings.DEFAULT;
    }

    public void save(UUID uuid, Settings settings) throws SQLException {
        try (Connection conn = db.getConnection()) {
            boolean exists;
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT 1 FROM uc_player_settings WHERE uuid = ?")) {
                check.setString(1, uuid.toString());
                try (ResultSet rs = check.executeQuery()) {
                    exists = rs.next();
                }
            }
            String sql = exists
                    ? "UPDATE uc_player_settings SET clan_chat = ?, tag_enabled = ? WHERE uuid = ?"
                    : "INSERT INTO uc_player_settings (clan_chat, tag_enabled, uuid) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, settings.clanChat() ? 1 : 0);
                ps.setInt(2, settings.tagEnabled() ? 1 : 0);
                ps.setString(3, uuid.toString());
                ps.executeUpdate();
            }
        }
    }
}
