package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.model.AllianceInvite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class AllianceInviteRepository {

    private final DatabaseManager db;

    public AllianceInviteRepository(DatabaseManager db) {
        this.db = db;
    }

    public void insert(AllianceInvite invite) throws SQLException {
        String sql = """
            INSERT INTO uc_alliance_invites (id, sender_clan_id, target_clan_id, sender_player,
                created_at, expires_at, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invite.getId().toString());
            ps.setString(2, invite.getSenderClanId().toString());
            ps.setString(3, invite.getTargetClanId().toString());
            ps.setString(4, invite.getSenderPlayer().toString());
            ps.setLong(5, invite.getCreatedAt());
            ps.setLong(6, invite.getExpiresAt());
            ps.setString(7, invite.getStatus().name());
            ps.executeUpdate();
        }
    }

    public void updateStatus(UUID id, AllianceInvite.Status status) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE uc_alliance_invites SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setString(2, id.toString());
            ps.executeUpdate();
        }
    }

    public Optional<AllianceInvite> findPendingBetween(UUID senderClanId, UUID targetClanId) throws SQLException {
        String sql = """
            SELECT * FROM uc_alliance_invites
            WHERE sender_clan_id = ? AND target_clan_id = ? AND status = 'PENDING'
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, senderClanId.toString());
            ps.setString(2, targetClanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean hasPendingEitherDirection(UUID clanA, UUID clanB) throws SQLException {
        String sql = """
            SELECT 1 FROM uc_alliance_invites
            WHERE status = 'PENDING' AND ((sender_clan_id = ? AND target_clan_id = ?) OR (sender_clan_id = ? AND target_clan_id = ?))
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clanA.toString());
            ps.setString(2, clanB.toString());
            ps.setString(3, clanB.toString());
            ps.setString(4, clanA.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void deleteByClan(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM uc_alliance_invites WHERE sender_clan_id = ? OR target_clan_id = ?")) {
            ps.setString(1, clanId.toString());
            ps.setString(2, clanId.toString());
            ps.executeUpdate();
        }
    }

    private AllianceInvite map(ResultSet rs) throws SQLException {
        return new AllianceInvite(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("sender_clan_id")),
                UUID.fromString(rs.getString("target_clan_id")),
                UUID.fromString(rs.getString("sender_player")),
                rs.getLong("created_at"),
                rs.getLong("expires_at"),
                AllianceInvite.Status.valueOf(rs.getString("status"))
        );
    }
}
