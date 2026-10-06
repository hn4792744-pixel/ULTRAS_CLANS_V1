package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.model.AllianceSettings;
import me.uc.hussein.ultrasclans.model.ClanAlliance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class AllianceRepository {

    private final DatabaseManager db;

    public AllianceRepository(DatabaseManager db) {
        this.db = db;
    }

    public void insert(ClanAlliance alliance) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO uc_clan_alliances (id, clan_a, clan_b, created_at) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, alliance.getId().toString());
            ps.setString(2, alliance.getClanA().toString());
            ps.setString(3, alliance.getClanB().toString());
            ps.setLong(4, alliance.getCreatedAt());
            ps.executeUpdate();
        }
        insertDefaultSettings(alliance.getId(), alliance.getClanA());
        insertDefaultSettings(alliance.getId(), alliance.getClanB());
    }

    private void insertDefaultSettings(UUID allianceId, UUID granterClanId) throws SQLException {
        AllianceSettings defaults = AllianceSettings.defaults();
        saveSettings(allianceId, granterClanId, defaults);
    }

    public void delete(UUID allianceId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_clan_alliances WHERE id = ?")) {
            ps.setString(1, allianceId.toString());
            ps.executeUpdate();
        }
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_alliance_settings WHERE alliance_id = ?")) {
            ps.setString(1, allianceId.toString());
            ps.executeUpdate();
        }
    }

    public void deleteAllForClan(UUID clanId) throws SQLException {
        for (ClanAlliance alliance : findByClan(clanId)) {
            delete(alliance.getId());
        }
    }

    public List<ClanAlliance> findByClan(UUID clanId) throws SQLException {
        List<ClanAlliance> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM uc_clan_alliances WHERE clan_a = ? OR clan_b = ?")) {
            ps.setString(1, clanId.toString());
            ps.setString(2, clanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    public Optional<ClanAlliance> findBetween(UUID clanA, UUID clanB) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM uc_clan_alliances WHERE (clan_a = ? AND clan_b = ?) OR (clan_a = ? AND clan_b = ?)")) {
            ps.setString(1, clanA.toString());
            ps.setString(2, clanB.toString());
            ps.setString(3, clanB.toString());
            ps.setString(4, clanA.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void saveSettings(UUID allianceId, UUID granterClanId, AllianceSettings settings) throws SQLException {
        try (Connection conn = db.getConnection()) {
            boolean exists;
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT 1 FROM uc_alliance_settings WHERE alliance_id = ? AND granter_clan_id = ?")) {
                check.setString(1, allianceId.toString());
                check.setString(2, granterClanId.toString());
                try (ResultSet rs = check.executeQuery()) {
                    exists = rs.next();
                }
            }
            String sql = exists
                    ? """
                      UPDATE uc_alliance_settings SET allow_spawn = ?, allow_warp = ?, allow_storage = ?,
                          allow_bank = ?, allow_member_list = ?, allow_clan_info = ?, prevent_pvp = ?
                      WHERE alliance_id = ? AND granter_clan_id = ?
                      """
                    : """
                      INSERT INTO uc_alliance_settings (allow_spawn, allow_warp, allow_storage, allow_bank,
                          allow_member_list, allow_clan_info, prevent_pvp, alliance_id, granter_clan_id)
                      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                      """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, settings.isAllowSpawn() ? 1 : 0);
                ps.setInt(2, settings.isAllowWarp() ? 1 : 0);
                ps.setInt(3, settings.isAllowStorage() ? 1 : 0);
                ps.setInt(4, settings.isAllowBank() ? 1 : 0);
                ps.setInt(5, settings.isAllowMemberList() ? 1 : 0);
                ps.setInt(6, settings.isAllowClanInfo() ? 1 : 0);
                ps.setInt(7, settings.isPreventPvp() ? 1 : 0);
                ps.setString(8, allianceId.toString());
                ps.setString(9, granterClanId.toString());
                ps.executeUpdate();
            }
        }
    }

    public AllianceSettings loadSettings(UUID allianceId, UUID granterClanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM uc_alliance_settings WHERE alliance_id = ? AND granter_clan_id = ?")) {
            ps.setString(1, allianceId.toString());
            ps.setString(2, granterClanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AllianceSettings(
                            rs.getInt("allow_spawn") == 1,
                            rs.getInt("allow_warp") == 1,
                            rs.getInt("allow_storage") == 1,
                            rs.getInt("allow_bank") == 1,
                            rs.getInt("allow_member_list") == 1,
                            rs.getInt("allow_clan_info") == 1,
                            rs.getInt("prevent_pvp") == 1
                    );
                }
            }
        }
        return AllianceSettings.defaults();
    }

    private ClanAlliance map(ResultSet rs) throws SQLException {
        return new ClanAlliance(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("clan_a")),
                UUID.fromString(rs.getString("clan_b")),
                rs.getLong("created_at")
        );
    }
}
