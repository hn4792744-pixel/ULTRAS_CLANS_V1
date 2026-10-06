package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;
import java.util.UUID;

/**
 * يسجل كل الأحداث الحساسة (راجع قسم 46 من المواصفات). كل سطر مرتبط
 * بمعرف ربط (correlation id) فريد يمنع تنفيذ نفس العملية مرتين.
 */
public final class AuditLogRepository {

    private final DatabaseManager db;

    public AuditLogRepository(DatabaseManager db) {
        this.db = db;
    }

    public void log(String correlationId, UUID actorUuid, UUID clanId, String action,
                     String amount, String beforeValue, String afterValue, String source) throws SQLException {
        String sql = """
            INSERT INTO uc_audit_logs (correlation_id, timestamp, actor_uuid, clan_id, action,
                amount, before_value, after_value, source)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, correlationId);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, actorUuid != null ? actorUuid.toString() : null);
            ps.setString(4, clanId != null ? clanId.toString() : null);
            ps.setString(5, action);
            ps.setString(6, amount);
            ps.setString(7, beforeValue);
            ps.setString(8, afterValue);
            ps.setString(9, source);
            ps.executeUpdate();
        }
    }

    public record Entry(long timestamp, String actor, String clanId, String action, String amount,
                         String before, String after, String source, String correlationId) {
    }

    /** بحث في السجلات: clanId أو action (يحتوي) اختياريان، الأحدث أولًا. */
    public List<Entry> search(UUID clanId, String actionContains, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM uc_audit_logs WHERE 1=1");
        if (clanId != null) sql.append(" AND clan_id = ?");
        if (actionContains != null && !actionContains.isBlank()) sql.append(" AND action LIKE ?");
        sql.append(" ORDER BY timestamp DESC LIMIT ?");
        List<Entry> result = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = 1;
            if (clanId != null) ps.setString(i++, clanId.toString());
            if (actionContains != null && !actionContains.isBlank()) ps.setString(i++, "%" + actionContains.toUpperCase() + "%");
            ps.setInt(i, Math.max(1, Math.min(limit, 100)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Entry(rs.getLong("timestamp"), rs.getString("actor_uuid"), rs.getString("clan_id"),
                            rs.getString("action"), rs.getString("amount"), rs.getString("before_value"),
                            rs.getString("after_value"), rs.getString("source"), rs.getString("correlation_id")));
                }
            }
        }
        return result;
    }

    public int purgeOlderThan(long cutoffMillis) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_audit_logs WHERE timestamp < ?")) {
            ps.setLong(1, cutoffMillis);
            return ps.executeUpdate();
        }
    }

    public int deleteAll() throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_audit_logs")) {
            return ps.executeUpdate();
        }
    }
}
