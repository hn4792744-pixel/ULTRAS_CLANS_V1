package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * يخزن آخر وقت مغادرة كلان وآخر محاولة إنشاء كلان لكل لاعب، بشكل دائم
 * في قاعدة البيانات (وليس فقط في الذاكرة) حتى لا يُلتف حول cooldown
 * الانضمام/الإنشاء بمجرد إعادة تشغيل السيرفر.
 */
public final class CooldownRepository {

    private final DatabaseManager db;

    public CooldownRepository(DatabaseManager db) {
        this.db = db;
    }

    public long getLastLeaveAt(UUID uuid) throws SQLException {
        return queryValue(uuid, "last_leave_at");
    }

    public long getLastCreateAttemptAt(UUID uuid) throws SQLException {
        return queryValue(uuid, "last_create_attempt_at");
    }

    public void setLastLeaveAt(UUID uuid, long timestamp) throws SQLException {
        upsert(uuid, "last_leave_at", timestamp);
    }

    public void setLastCreateAttemptAt(UUID uuid, long timestamp) throws SQLException {
        upsert(uuid, "last_create_attempt_at", timestamp);
    }

    private long queryValue(UUID uuid, String column) throws SQLException {
        String sql = "SELECT " + column + " FROM uc_player_cooldowns WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private void upsert(UUID uuid, String column, long value) throws SQLException {
        try (Connection conn = db.getConnection()) {
            String select = "SELECT 1 FROM uc_player_cooldowns WHERE uuid = ?";
            boolean exists;
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }
            if (exists) {
                String update = "UPDATE uc_player_cooldowns SET " + column + " = ? WHERE uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setLong(1, value);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            } else {
                String insert = "INSERT INTO uc_player_cooldowns (uuid, " + column + ") VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setString(1, uuid.toString());
                    ps.setLong(2, value);
                    ps.executeUpdate();
                }
            }
        }
    }
}
