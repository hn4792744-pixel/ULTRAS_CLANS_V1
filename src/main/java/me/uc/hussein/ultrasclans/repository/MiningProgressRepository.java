package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * يخزن عدّاد الكتل المكسورة تراكميًا لكل لاعب/خام (لحساب CP التعدين -
 * قسم 10). يُحمَّل عند دخول اللاعب ويُحفظ دوريًا/عند الخروج فقط (وليس
 * عند كل كتلة) تفاديًا لأي ضغط على قاعدة البيانات (قسم 59).
 */
public final class MiningProgressRepository {

    private final DatabaseManager db;

    public MiningProgressRepository(DatabaseManager db) {
        this.db = db;
    }

    public Map<String, Integer> loadAll(UUID uuid) throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT material, count FROM uc_mining_progress WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("material"), rs.getInt("count"));
                }
            }
        }
        return result;
    }

    /** يحفظ كل عدّادات لاعب دفعة واحدة (استبدال كامل - يُستدعى عند الخروج فقط). */
    public void saveAll(UUID uuid, Map<String, Integer> counts) throws SQLException {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delete = conn.prepareStatement(
                    "DELETE FROM uc_mining_progress WHERE uuid = ?")) {
                delete.setString(1, uuid.toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO uc_mining_progress (uuid, material, count) VALUES (?, ?, ?)")) {
                for (var entry : counts.entrySet()) {
                    if (entry.getValue() <= 0) continue;
                    insert.setString(1, uuid.toString());
                    insert.setString(2, entry.getKey());
                    insert.setInt(3, entry.getValue());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            conn.commit();
            conn.setAutoCommit(true);
        }
    }
}
