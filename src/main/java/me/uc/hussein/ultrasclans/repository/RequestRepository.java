package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.model.JoinRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class RequestRepository {

    private final DatabaseManager db;

    public RequestRepository(DatabaseManager db) {
        this.db = db;
    }

    public void insert(JoinRequest request) throws SQLException {
        String sql = """
            INSERT INTO uc_clan_requests (id, clan_id, requester_uuid, created_at, expires_at, status)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, request.getId().toString());
            ps.setString(2, request.getClanId().toString());
            ps.setString(3, request.getRequesterUuid().toString());
            ps.setLong(4, request.getCreatedAt());
            ps.setLong(5, request.getExpiresAt());
            ps.setString(6, request.getStatus().name());
            ps.executeUpdate();
        }
    }

    public void updateStatus(UUID id, JoinRequest.Status status) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE uc_clan_requests SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setString(2, id.toString());
            ps.executeUpdate();
        }
    }

    /** يرفض كل الطلبات الأخرى المعلقة لنفس اللاعب (يُستدعى عند قبول طلب واحد). */
    public void denyOtherPendingForPlayer(UUID requesterUuid, UUID acceptedRequestId) throws SQLException {
        String sql = "UPDATE uc_clan_requests SET status = 'DENIED' WHERE requester_uuid = ? AND status = 'PENDING' AND id != ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requesterUuid.toString());
            ps.setString(2, acceptedRequestId.toString());
            ps.executeUpdate();
        }
    }

    public void deleteByClan(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_clan_requests WHERE clan_id = ?")) {
            ps.setString(1, clanId.toString());
            ps.executeUpdate();
        }
    }

    /** يحوّل كل طلبات الانضمام المعلقة المنتهية الصلاحية إلى EXPIRED - يُستدعى دوريًا. */
    public int expireOverdue() throws SQLException {
        String sql = "UPDATE uc_clan_requests SET status = 'EXPIRED' WHERE status = 'PENDING' AND expires_at < ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            return ps.executeUpdate();
        }
    }

    public List<JoinRequest> findPendingForClan(UUID clanId) throws SQLException {
        List<JoinRequest> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM uc_clan_requests WHERE clan_id = ? AND status = 'PENDING'")) {
            ps.setString(1, clanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public boolean hasPendingRequest(UUID clanId, UUID requesterUuid) throws SQLException {
        String sql = "SELECT 1 FROM uc_clan_requests WHERE clan_id = ? AND requester_uuid = ? AND status = 'PENDING'";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clanId.toString());
            ps.setString(2, requesterUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int countPendingForClan(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM uc_clan_requests WHERE clan_id = ? AND status = 'PENDING'")) {
            ps.setString(1, clanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Optional<JoinRequest> findById(UUID id) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_clan_requests WHERE id = ?")) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    private JoinRequest map(ResultSet rs) throws SQLException {
        return new JoinRequest(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("clan_id")),
                UUID.fromString(rs.getString("requester_uuid")),
                rs.getLong("created_at"),
                rs.getLong("expires_at"),
                JoinRequest.Status.valueOf(rs.getString("status"))
        );
    }
}
