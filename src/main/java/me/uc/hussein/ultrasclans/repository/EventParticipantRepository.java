package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.event.EventParticipant;
import me.uc.hussein.ultrasclans.event.ParticipantState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EventParticipantRepository {

    private final DatabaseManager db;

    public EventParticipantRepository(DatabaseManager db) {
        this.db = db;
    }

    public void insertAll(List<EventParticipant> participants) throws SQLException {
        String sql = """
            INSERT INTO uc_event_participants (event_id, player_uuid, clan_id, state,
                pre_world, pre_x, pre_y, pre_z, pre_yaw, pre_pitch, disconnected_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (EventParticipant p : participants) {
                ps.setString(1, p.getEventId().toString());
                ps.setString(2, p.getPlayerUuid().toString());
                ps.setString(3, p.getClanId().toString());
                ps.setString(4, p.getState().name());
                ps.setString(5, p.getPreWorld());
                ps.setDouble(6, p.getPreX());
                ps.setDouble(7, p.getPreY());
                ps.setDouble(8, p.getPreZ());
                ps.setFloat(9, p.getPreYaw());
                ps.setFloat(10, p.getPrePitch());
                if (p.getDisconnectedAt() == null) {
                    ps.setNull(11, java.sql.Types.BIGINT);
                } else {
                    ps.setLong(11, p.getDisconnectedAt());
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void updateState(UUID eventId, UUID playerUuid, ParticipantState state, Long disconnectedAt) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE uc_event_participants SET state = ?, disconnected_at = ? WHERE event_id = ? AND player_uuid = ?")) {
            ps.setString(1, state.name());
            if (disconnectedAt == null) ps.setNull(2, java.sql.Types.BIGINT); else ps.setLong(2, disconnectedAt);
            ps.setString(3, eventId.toString());
            ps.setString(4, playerUuid.toString());
            ps.executeUpdate();
        }
    }

    public List<EventParticipant> findByEvent(UUID eventId) throws SQLException {
        List<EventParticipant> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_event_participants WHERE event_id = ?")) {
            ps.setString(1, eventId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    public void deleteByEvent(UUID eventId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_event_participants WHERE event_id = ?")) {
            ps.setString(1, eventId.toString());
            ps.executeUpdate();
        }
    }

    private EventParticipant map(ResultSet rs) throws SQLException {
        long disc = rs.getLong("disconnected_at");
        Long discBoxed = rs.wasNull() ? null : disc;

        return new EventParticipant(
                UUID.fromString(rs.getString("event_id")),
                UUID.fromString(rs.getString("player_uuid")),
                UUID.fromString(rs.getString("clan_id")),
                ParticipantState.valueOf(rs.getString("state")),
                rs.getString("pre_world"),
                rs.getDouble("pre_x"), rs.getDouble("pre_y"), rs.getDouble("pre_z"),
                rs.getFloat("pre_yaw"), rs.getFloat("pre_pitch"),
                discBoxed
        );
    }
}
