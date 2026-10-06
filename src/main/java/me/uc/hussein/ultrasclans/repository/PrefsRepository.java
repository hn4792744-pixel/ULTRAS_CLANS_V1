package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.model.PlayerPrefs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** صف لكل لاعب غيّر أي تفضيل فقط؛ من لا صف له يستخدم PlayerPrefs.DEFAULT. */
public final class PrefsRepository {

    private final DatabaseManager db;

    public PrefsRepository(DatabaseManager db) {
        this.db = db;
    }

    public Map<UUID, PlayerPrefs> loadAll() throws SQLException {
        Map<UUID, PlayerPrefs> result = new HashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_player_prefs");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String lang = rs.getString("language");
                result.put(UUID.fromString(rs.getString("uuid")), new PlayerPrefs(
                        lang == null ? "" : lang,
                        rs.getInt("messages") == 1, rs.getInt("sounds") == 1, rs.getInt("hud") == 1,
                        rs.getInt("accept_invites") == 1, rs.getInt("accept_tpa") == 1,
                        rs.getInt("accept_alliance") == 1, rs.getInt("accept_join") == 1,
                        rs.getInt("accept_challenges") == 1));
            }
        }
        return result;
    }

    public void save(UUID uuid, PlayerPrefs p) throws SQLException {
        try (Connection conn = db.getConnection()) {
            boolean exists;
            try (PreparedStatement check = conn.prepareStatement("SELECT 1 FROM uc_player_prefs WHERE uuid = ?")) {
                check.setString(1, uuid.toString());
                try (ResultSet rs = check.executeQuery()) {
                    exists = rs.next();
                }
            }
            String sql = exists
                    ? "UPDATE uc_player_prefs SET language = ?, messages = ?, sounds = ?, hud = ?, accept_invites = ?, "
                      + "accept_tpa = ?, accept_alliance = ?, accept_join = ?, accept_challenges = ? WHERE uuid = ?"
                    : "INSERT INTO uc_player_prefs (language, messages, sounds, hud, accept_invites, accept_tpa, "
                      + "accept_alliance, accept_join, accept_challenges, uuid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.language());
                ps.setInt(2, p.messages() ? 1 : 0);
                ps.setInt(3, p.sounds() ? 1 : 0);
                ps.setInt(4, p.hud() ? 1 : 0);
                ps.setInt(5, p.invites() ? 1 : 0);
                ps.setInt(6, p.tpa() ? 1 : 0);
                ps.setInt(7, p.alliance() ? 1 : 0);
                ps.setInt(8, p.joinRequests() ? 1 : 0);
                ps.setInt(9, p.challenges() ? 1 : 0);
                ps.setString(10, uuid.toString());
                ps.executeUpdate();
            }
        }
    }
}
