package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.model.Clan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * طبقة الوصول لبيانات الكلانات في قاعدة البيانات.
 * جميع الدوال هنا Blocking (JDBC) ويجب استدعاؤها فقط من Thread غير
 * رئيسي (عبر DatabaseManager#getExecutor()), أبدًا من الـmain thread.
 */
public final class ClanRepository {

    private final DatabaseManager db;

    public ClanRepository(DatabaseManager db) {
        this.db = db;
    }

    public void insert(Clan clan) throws SQLException {
        String sql = """
            INSERT INTO uc_clans (id, name, name_lower, owner_uuid, color, is_public,
                member_capacity, bank_balance, bank_capacity, cp, clan_level, storage_level,
                warp_capacity, alliance_capacity, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clan.getId().toString());
            ps.setString(2, clan.getName());
            ps.setString(3, clan.getName().toLowerCase());
            ps.setString(4, clan.getOwnerUuid().toString());
            ps.setString(5, clan.getColor());
            ps.setInt(6, clan.isPublic() ? 1 : 0);
            ps.setInt(7, clan.getMemberCapacity());
            ps.setDouble(8, clan.getBankBalance());
            ps.setDouble(9, clan.getBankCapacity());
            ps.setLong(10, clan.getCp());
            ps.setInt(11, clan.getClanLevel());
            ps.setInt(12, clan.getStorageLevel());
            ps.setInt(13, clan.getWarpCapacity());
            ps.setInt(14, clan.getAllianceCapacity());
            ps.setLong(15, clan.getCreatedAt());
            ps.executeUpdate();
        }
    }

    public void update(Clan clan) throws SQLException {
        String sql = """
            UPDATE uc_clans SET name = ?, name_lower = ?, owner_uuid = ?, color = ?, is_public = ?,
                member_capacity = ?, bank_balance = ?, bank_capacity = ?, cp = ?, clan_level = ?,
                storage_level = ?, warp_capacity = ?, alliance_capacity = ?
            WHERE id = ?
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clan.getName());
            ps.setString(2, clan.getName().toLowerCase());
            ps.setString(3, clan.getOwnerUuid().toString());
            ps.setString(4, clan.getColor());
            ps.setInt(5, clan.isPublic() ? 1 : 0);
            ps.setInt(6, clan.getMemberCapacity());
            ps.setDouble(7, clan.getBankBalance());
            ps.setDouble(8, clan.getBankCapacity());
            ps.setLong(9, clan.getCp());
            ps.setInt(10, clan.getClanLevel());
            ps.setInt(11, clan.getStorageLevel());
            ps.setInt(12, clan.getWarpCapacity());
            ps.setInt(13, clan.getAllianceCapacity());
            ps.setString(14, clan.getId().toString());
            ps.executeUpdate();
        }
    }

    public void delete(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_clans WHERE id = ?")) {
            ps.setString(1, clanId.toString());
            ps.executeUpdate();
        }
    }

    public Optional<Clan> findById(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_clans WHERE id = ?")) {
            ps.setString(1, clanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Clan> findByNameLower(String nameLower) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_clans WHERE name_lower = ?")) {
            ps.setString(1, nameLower);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean existsByNameLower(String nameLower) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM uc_clans WHERE name_lower = ?")) {
            ps.setString(1, nameLower);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Clan> findAll() throws SQLException {
        List<Clan> clans = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_clans ORDER BY cp DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clans.add(map(rs));
            }
        }
        return clans;
    }

    private Clan map(ResultSet rs) throws SQLException {
        return new Clan(
                UUID.fromString(rs.getString("id")),
                rs.getString("name"),
                UUID.fromString(rs.getString("owner_uuid")),
                rs.getString("color"),
                rs.getInt("is_public") == 1,
                rs.getInt("member_capacity"),
                rs.getDouble("bank_balance"),
                rs.getDouble("bank_capacity"),
                rs.getLong("cp"),
                rs.getInt("clan_level"),
                rs.getInt("storage_level"),
                rs.getInt("warp_capacity"),
                rs.getInt("alliance_capacity"),
                rs.getLong("created_at")
        );
    }
}
