package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.model.MemberHistorySnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class HistoryRepository {

    private final DatabaseManager db;

    public HistoryRepository(DatabaseManager db) {
        this.db = db;
    }

    /** يستبدل أي لقطة سابقة لنفس اللاعب (لا يمكن أن يملك أكثر من لقطة واحدة نشطة). */
    public void save(MemberHistorySnapshot snapshot) throws SQLException {
        deleteFor(snapshot.getUuid());
        String sql = """
            INSERT INTO uc_member_history (uuid, clan_id, clan_name, rank_key, member_level, member_xp,
                kills, deaths, contribution, left_at, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, snapshot.getUuid().toString());
            ps.setString(2, snapshot.getClanId().toString());
            ps.setString(3, snapshot.getClanName());
            ps.setString(4, snapshot.getRankKey());
            ps.setInt(5, snapshot.getMemberLevel());
            ps.setInt(6, snapshot.getMemberXp());
            ps.setInt(7, snapshot.getKills());
            ps.setInt(8, snapshot.getDeaths());
            ps.setInt(9, snapshot.getContribution());
            ps.setLong(10, snapshot.getLeftAt());
            ps.setLong(11, snapshot.getExpiresAt());
            ps.executeUpdate();
        }
    }

    public Optional<MemberHistorySnapshot> find(UUID uuid) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_member_history WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void deleteFor(UUID uuid) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_member_history WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    /** يحذف كل اللقطات المنتهية الصلاحية - يُستدعى دوريًا (housekeeping task). */
    public int purgeExpired() throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_member_history WHERE expires_at < ?")) {
            ps.setLong(1, System.currentTimeMillis());
            return ps.executeUpdate();
        }
    }

    private MemberHistorySnapshot map(ResultSet rs) throws SQLException {
        return new MemberHistorySnapshot(
                UUID.fromString(rs.getString("uuid")),
                UUID.fromString(rs.getString("clan_id")),
                rs.getString("clan_name"),
                rs.getString("rank_key"),
                rs.getInt("member_level"),
                rs.getInt("member_xp"),
                rs.getInt("kills"),
                rs.getInt("deaths"),
                rs.getInt("contribution"),
                rs.getLong("left_at"),
                rs.getLong("expires_at")
        );
    }
}
