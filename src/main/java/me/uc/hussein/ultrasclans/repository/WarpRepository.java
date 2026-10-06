package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.model.ClanWarp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class WarpRepository {

    private final DatabaseManager db;

    public WarpRepository(DatabaseManager db) {
        this.db = db;
    }

    public void insert(ClanWarp warp) throws SQLException {
        String sql = """
            INSERT INTO uc_clan_warps (clan_id, warp_id, world, x, y, z, yaw, pitch, created_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, warp.getClanId().toString());
            ps.setInt(2, warp.getWarpId());
            ps.setString(3, warp.getWorld());
            ps.setDouble(4, warp.getX());
            ps.setDouble(5, warp.getY());
            ps.setDouble(6, warp.getZ());
            ps.setFloat(7, warp.getYaw());
            ps.setFloat(8, warp.getPitch());
            ps.setString(9, warp.getCreatedBy() != null ? warp.getCreatedBy().toString() : null);
            ps.setLong(10, warp.getCreatedAt());
            ps.executeUpdate();
        }
    }

    public void delete(UUID clanId, int warpId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM uc_clan_warps WHERE clan_id = ? AND warp_id = ?")) {
            ps.setString(1, clanId.toString());
            ps.setInt(2, warpId);
            ps.executeUpdate();
        }
    }

    public void deleteByClan(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_clan_warps WHERE clan_id = ?")) {
            ps.setString(1, clanId.toString());
            ps.executeUpdate();
        }
    }

    public List<ClanWarp> findByClan(UUID clanId) throws SQLException {
        List<ClanWarp> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM uc_clan_warps WHERE clan_id = ? ORDER BY warp_id ASC")) {
            ps.setString(1, clanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    public Optional<ClanWarp> find(UUID clanId, int warpId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM uc_clan_warps WHERE clan_id = ? AND warp_id = ?")) {
            ps.setString(1, clanId.toString());
            ps.setInt(2, warpId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public int countByClan(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM uc_clan_warps WHERE clan_id = ?")) {
            ps.setString(1, clanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private ClanWarp map(ResultSet rs) throws SQLException {
        return new ClanWarp(
                UUID.fromString(rs.getString("clan_id")),
                rs.getInt("warp_id"),
                rs.getString("world"),
                rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                rs.getFloat("yaw"), rs.getFloat("pitch"),
                rs.getString("created_by") != null ? UUID.fromString(rs.getString("created_by")) : null,
                rs.getLong("created_at")
        );
    }
}
