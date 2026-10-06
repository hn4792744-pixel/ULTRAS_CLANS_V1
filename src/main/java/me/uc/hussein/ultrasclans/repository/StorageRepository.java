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
 * يخزن محتوى Storage كل كلان كسلاسل نصية (Base64 لكل ItemStack مُسلسل).
 * كل slot له سطر منفصل، فارغ = لا يوجد سطر لهذا الـslot.
 */
public final class StorageRepository {

    private final DatabaseManager db;

    public StorageRepository(DatabaseManager db) {
        this.db = db;
    }

    public Map<Integer, String> loadAll(UUID clanId) throws SQLException {
        Map<Integer, String> result = new HashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT slot_index, item_data FROM uc_clan_storage WHERE clan_id = ?")) {
            ps.setString(1, clanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("slot_index"), rs.getString("item_data"));
                }
            }
        }
        return result;
    }

    /** يحفظ أو يحدّث slot واحد. تمرير null لـitemData يحذف الـslot (يجعله فارغًا). */
    public void saveSlot(UUID clanId, int slotIndex, String itemDataOrNull) throws SQLException {
        if (itemDataOrNull == null) {
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM uc_clan_storage WHERE clan_id = ? AND slot_index = ?")) {
                ps.setString(1, clanId.toString());
                ps.setInt(2, slotIndex);
                ps.executeUpdate();
            }
            return;
        }

        try (Connection conn = db.getConnection()) {
            boolean exists;
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT 1 FROM uc_clan_storage WHERE clan_id = ? AND slot_index = ?")) {
                check.setString(1, clanId.toString());
                check.setInt(2, slotIndex);
                try (ResultSet rs = check.executeQuery()) {
                    exists = rs.next();
                }
            }
            if (exists) {
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE uc_clan_storage SET item_data = ? WHERE clan_id = ? AND slot_index = ?")) {
                    update.setString(1, itemDataOrNull);
                    update.setString(2, clanId.toString());
                    update.setInt(3, slotIndex);
                    update.executeUpdate();
                }
            } else {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO uc_clan_storage (clan_id, slot_index, item_data) VALUES (?, ?, ?)")) {
                    insert.setString(1, clanId.toString());
                    insert.setInt(2, slotIndex);
                    insert.setString(3, itemDataOrNull);
                    insert.executeUpdate();
                }
            }
        }
    }

    public void deleteByClan(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_clan_storage WHERE clan_id = ?")) {
            ps.setString(1, clanId.toString());
            ps.executeUpdate();
        }
    }
}
