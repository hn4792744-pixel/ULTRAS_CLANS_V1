package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.model.ClanRankDefinition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RankRepository {

    private final DatabaseManager db;

    public RankRepository(DatabaseManager db) {
        this.db = db;
    }

    public void insert(ClanRankDefinition rank) throws SQLException {
        String sql = """
            INSERT INTO uc_clan_ranks (clan_id, rank_key, name, display, color, priority,
                is_protected, is_default, permissions)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, rank);
            ps.executeUpdate();
        }
    }

    public void update(ClanRankDefinition rank) throws SQLException {
        String sql = """
            UPDATE uc_clan_ranks SET name = ?, display = ?, color = ?, priority = ?,
                is_default = ?, permissions = ?
            WHERE clan_id = ? AND rank_key = ?
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rank.getName());
            ps.setString(2, rank.getDisplay());
            ps.setString(3, rank.getColor());
            ps.setInt(4, rank.getPriority());
            ps.setInt(5, rank.isDefaultRank() ? 1 : 0);
            ps.setString(6, String.join(",", rank.getPermissions()));
            ps.setString(7, rank.getClanId().toString());
            ps.setString(8, rank.getRankKey());
            ps.executeUpdate();
        }
    }

    public void delete(UUID clanId, String rankKey) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM uc_clan_ranks WHERE clan_id = ? AND rank_key = ?")) {
            ps.setString(1, clanId.toString());
            ps.setString(2, rankKey);
            ps.executeUpdate();
        }
    }

    public void deleteByClan(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_clan_ranks WHERE clan_id = ?")) {
            ps.setString(1, clanId.toString());
            ps.executeUpdate();
        }
    }

    public List<ClanRankDefinition> findByClan(UUID clanId) throws SQLException {
        List<ClanRankDefinition> ranks = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM uc_clan_ranks WHERE clan_id = ? ORDER BY priority DESC")) {
            ps.setString(1, clanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ranks.add(map(rs, clanId));
                }
            }
        }
        return ranks;
    }

    public int countByClan(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM uc_clan_ranks WHERE clan_id = ?")) {
            ps.setString(1, clanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void bind(PreparedStatement ps, ClanRankDefinition rank) throws SQLException {
        ps.setString(1, rank.getClanId().toString());
        ps.setString(2, rank.getRankKey());
        ps.setString(3, rank.getName());
        ps.setString(4, rank.getDisplay());
        ps.setString(5, rank.getColor());
        ps.setInt(6, rank.getPriority());
        ps.setInt(7, rank.isProtectedRank() ? 1 : 0);
        ps.setInt(8, rank.isDefaultRank() ? 1 : 0);
        ps.setString(9, String.join(",", rank.getPermissions()));
    }

    private ClanRankDefinition map(ResultSet rs, UUID clanId) throws SQLException {
        String permsRaw = rs.getString("permissions");
        Set<String> permissions = new LinkedHashSet<>();
        if (permsRaw != null && !permsRaw.isBlank()) {
            permissions.addAll(List.of(permsRaw.split(",")));
        }
        return new ClanRankDefinition(
                clanId,
                rs.getString("rank_key"),
                rs.getString("name"),
                rs.getString("display"),
                rs.getString("color"),
                rs.getInt("priority"),
                rs.getInt("is_protected") == 1,
                rs.getInt("is_default") == 1,
                permissions
        );
    }
}
