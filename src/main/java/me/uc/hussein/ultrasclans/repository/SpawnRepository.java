package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.model.ClanSpawn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class SpawnRepository {

    private final DatabaseManager db;

    public SpawnRepository(DatabaseManager db) {
        this.db = db;
    }

    public void upsert(ClanSpawn spawn) throws SQLException {
        delete(spawn.getClanId());
        String sql = """
            INSERT INTO uc_clan_spawns (clan_id, world, x, y, z, yaw, pitch)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, spawn.getClanId().toString());
            ps.setString(2, spawn.getWorld());
            ps.setDouble(3, spawn.getX());
            ps.setDouble(4, spawn.getY());
            ps.setDouble(5, spawn.getZ());
            ps.setFloat(6, spawn.getYaw());
            ps.setFloat(7, spawn.getPitch());
            ps.executeUpdate();
        }
    }

    public void delete(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_clan_spawns WHERE clan_id = ?")) {
            ps.setString(1, clanId.toString());
            ps.executeUpdate();
        }
    }

    public Optional<ClanSpawn> find(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_clan_spawns WHERE clan_id = ?")) {
            ps.setString(1, clanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ClanSpawn(
                            clanId, rs.getString("world"),
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            rs.getFloat("yaw"), rs.getFloat("pitch")));
                }
            }
        }
        return Optional.empty();
    }
}
