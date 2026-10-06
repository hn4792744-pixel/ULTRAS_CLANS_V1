package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * يخزن نقاط الساحة الإدارية كإحداثيات خام فقط (بلا Bukkit API هنا - نفس
 * قاعدة EventParticipantRepository). صف واحد فقط دائمًا (id=1).
 */
public final class AdminArenaRepository {

    public record Point(String world, double x, double y, double z, float yaw, float pitch) {
    }

    public record ArenaRow(Point event1, Point event2, Point rest1Corner, Point rest2Corner) {
    }

    private final DatabaseManager db;

    public AdminArenaRepository(DatabaseManager db) {
        this.db = db;
    }

    public ArenaRow load() throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_admin_arena WHERE id = 1");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return new ArenaRow(null, null, null, null);
            }
            return new ArenaRow(
                    readPoint(rs, "event1", true),
                    readPoint(rs, "event2", true),
                    readPoint(rs, "rest1", false),
                    readPoint(rs, "rest2", false)
            );
        }
    }

    private Point readPoint(ResultSet rs, String prefix, boolean hasRotation) throws SQLException {
        String world = rs.getString(prefix + "_world");
        if (world == null) {
            return null;
        }
        double x = rs.getDouble(prefix + "_x");
        double y = rs.getDouble(prefix + "_y");
        double z = rs.getDouble(prefix + "_z");
        float yaw = hasRotation ? rs.getFloat(prefix + "_yaw") : 0f;
        float pitch = hasRotation ? rs.getFloat(prefix + "_pitch") : 0f;
        return new Point(world, x, y, z, yaw, pitch);
    }

    public void saveEvent1(Point point) throws SQLException {
        upsertColumn("event1_world", "event1_x", "event1_y", "event1_z", "event1_yaw", "event1_pitch", point, true);
    }

    public void saveEvent2(Point point) throws SQLException {
        upsertColumn("event2_world", "event2_x", "event2_y", "event2_z", "event2_yaw", "event2_pitch", point, true);
    }

    public void saveRest1(Point point) throws SQLException {
        upsertColumn("rest1_world", "rest1_x", "rest1_y", "rest1_z", null, null, point, false);
    }

    public void saveRest2(Point point) throws SQLException {
        upsertColumn("rest2_world", "rest2_x", "rest2_y", "rest2_z", null, null, point, false);
    }

    private void upsertColumn(String worldCol, String xCol, String yCol, String zCol,
                               String yawCol, String pitchCol, Point point, boolean hasRotation) throws SQLException {
        try (Connection conn = db.getConnection()) {
            boolean exists;
            try (PreparedStatement check = conn.prepareStatement("SELECT 1 FROM uc_admin_arena WHERE id = 1")) {
                try (ResultSet rs = check.executeQuery()) {
                    exists = rs.next();
                }
            }
            if (!exists) {
                try (PreparedStatement insert = conn.prepareStatement("INSERT INTO uc_admin_arena (id) VALUES (1)")) {
                    insert.executeUpdate();
                }
            }
            String cols = hasRotation
                    ? worldCol + " = ?, " + xCol + " = ?, " + yCol + " = ?, " + zCol + " = ?, " + yawCol + " = ?, " + pitchCol + " = ?"
                    : worldCol + " = ?, " + xCol + " = ?, " + yCol + " = ?, " + zCol + " = ?";
            try (PreparedStatement ps = conn.prepareStatement("UPDATE uc_admin_arena SET " + cols + " WHERE id = 1")) {
                int i = 1;
                ps.setString(i++, point.world());
                ps.setDouble(i++, point.x());
                ps.setDouble(i++, point.y());
                ps.setDouble(i++, point.z());
                if (hasRotation) {
                    ps.setFloat(i++, point.yaw());
                    ps.setFloat(i, point.pitch());
                }
                ps.executeUpdate();
            }
        }
    }
}
