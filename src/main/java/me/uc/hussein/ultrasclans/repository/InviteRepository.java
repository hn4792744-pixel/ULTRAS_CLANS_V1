package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.model.ClanInvite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class InviteRepository {

    private final DatabaseManager db;

    public InviteRepository(DatabaseManager db) {
        this.db = db;
    }

    public void insert(ClanInvite invite) throws SQLException {
        String sql = """
            INSERT INTO uc_clan_invites (id, clan_id, sender_uuid, receiver_uuid, created_at, expires_at, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invite.getId().toString());
            ps.setString(2, invite.getClanId().toString());
            ps.setString(3, invite.getSenderUuid().toString());
            ps.setString(4, invite.getReceiverUuid().toString());
            ps.setLong(5, invite.getCreatedAt());
            ps.setLong(6, invite.getExpiresAt());
            ps.setString(7, invite.getStatus().name());
            ps.executeUpdate();
        }
    }

    public void updateStatus(UUID id, ClanInvite.Status status) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE uc_clan_invites SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setString(2, id.toString());
            ps.executeUpdate();
        }
    }

    public void deleteByClan(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_clan_invites WHERE clan_id = ?")) {
            ps.setString(1, clanId.toString());
            ps.executeUpdate();
        }
    }

    /** يحوّل كل الدعوات المعلقة المنتهية الصلاحية إلى EXPIRED - يُستدعى دوريًا. */
    public int expireOverdue() throws SQLException {
        String sql = "UPDATE uc_clan_invites SET status = 'EXPIRED' WHERE status = 'PENDING' AND expires_at < ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            return ps.executeUpdate();
        }
    }

    public List<ClanInvite> findPendingForReceiver(UUID receiverUuid) throws SQLException {
        return queryList("SELECT * FROM uc_clan_invites WHERE receiver_uuid = ? AND status = 'PENDING'", receiverUuid);
    }

    public List<ClanInvite> findPendingForClan(UUID clanId) throws SQLException {
        return queryListByClan("SELECT * FROM uc_clan_invites WHERE clan_id = ? AND status = 'PENDING'", clanId);
    }

    public Optional<ClanInvite> findPendingForClanAndReceiver(UUID clanId, UUID receiverUuid) throws SQLException {
        String sql = "SELECT * FROM uc_clan_invites WHERE clan_id = ? AND receiver_uuid = ? AND status = 'PENDING'";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clanId.toString());
            ps.setString(2, receiverUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean hasPendingInvite(UUID clanId, UUID receiverUuid) throws SQLException {
        String sql = "SELECT 1 FROM uc_clan_invites WHERE clan_id = ? AND receiver_uuid = ? AND status = 'PENDING'";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clanId.toString());
            ps.setString(2, receiverUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int countPendingForClan(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM uc_clan_invites WHERE clan_id = ? AND status = 'PENDING'")) {
            ps.setString(1, clanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Optional<ClanInvite> findById(UUID id) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_clan_invites WHERE id = ?")) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    private List<ClanInvite> queryList(String sql, UUID param) throws SQLException {
        List<ClanInvite> list = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private List<ClanInvite> queryListByClan(String sql, UUID clanId) throws SQLException {
        return queryList(sql, clanId);
    }

    private ClanInvite map(ResultSet rs) throws SQLException {
        return new ClanInvite(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("clan_id")),
                UUID.fromString(rs.getString("sender_uuid")),
                UUID.fromString(rs.getString("receiver_uuid")),
                rs.getLong("created_at"),
                rs.getLong("expires_at"),
                ClanInvite.Status.valueOf(rs.getString("status"))
        );
    }
}
