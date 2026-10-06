package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.model.ClanMember;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class MemberRepository {

    private final DatabaseManager db;

    public MemberRepository(DatabaseManager db) {
        this.db = db;
    }

    public void insert(ClanMember member) throws SQLException {
        String sql = """
            INSERT INTO uc_clan_members (uuid, clan_id, rank_key, member_level, member_xp,
                kills, deaths, contribution, joined_at, last_online)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, member);
            ps.executeUpdate();
        }
    }

    public void update(ClanMember member) throws SQLException {
        String sql = """
            UPDATE uc_clan_members SET clan_id = ?, rank_key = ?, member_level = ?, member_xp = ?,
                kills = ?, deaths = ?, contribution = ?, joined_at = ?, last_online = ?
            WHERE uuid = ?
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, member.getClanId().toString());
            ps.setString(2, member.getRankKey());
            ps.setInt(3, member.getMemberLevel());
            ps.setInt(4, member.getMemberXp());
            ps.setInt(5, member.getKills());
            ps.setInt(6, member.getDeaths());
            ps.setInt(7, member.getContribution());
            ps.setLong(8, member.getJoinedAt());
            ps.setLong(9, member.getLastOnline());
            ps.setString(10, member.getUuid().toString());
            ps.executeUpdate();
        }
    }

    public void delete(UUID uuid) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_clan_members WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void deleteByClan(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_clan_members WHERE clan_id = ?")) {
            ps.setString(1, clanId.toString());
            ps.executeUpdate();
        }
    }

    public Optional<ClanMember> findByUuid(UUID uuid) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_clan_members WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<ClanMember> findAll() throws SQLException {
        List<ClanMember> members = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_clan_members");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                members.add(map(rs));
            }
        }
        return members;
    }

    public List<ClanMember> findByClan(UUID clanId) throws SQLException {
        List<ClanMember> members = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_clan_members WHERE clan_id = ?")) {
            ps.setString(1, clanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(map(rs));
                }
            }
        }
        return members;
    }

    public int countByClan(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM uc_clan_members WHERE clan_id = ?")) {
            ps.setString(1, clanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void bind(PreparedStatement ps, ClanMember member) throws SQLException {
        ps.setString(1, member.getUuid().toString());
        ps.setString(2, member.getClanId().toString());
        ps.setString(3, member.getRankKey());
        ps.setInt(4, member.getMemberLevel());
        ps.setInt(5, member.getMemberXp());
        ps.setInt(6, member.getKills());
        ps.setInt(7, member.getDeaths());
        ps.setInt(8, member.getContribution());
        ps.setLong(9, member.getJoinedAt());
        ps.setLong(10, member.getLastOnline());
    }

    private ClanMember map(ResultSet rs) throws SQLException {
        return new ClanMember(
                UUID.fromString(rs.getString("uuid")),
                UUID.fromString(rs.getString("clan_id")),
                rs.getString("rank_key"),
                rs.getInt("member_level"),
                rs.getInt("member_xp"),
                rs.getInt("kills"),
                rs.getInt("deaths"),
                rs.getInt("contribution"),
                rs.getLong("joined_at"),
                rs.getLong("last_online")
        );
    }
}
