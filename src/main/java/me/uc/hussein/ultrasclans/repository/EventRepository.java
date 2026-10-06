package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.event.ClanEvent;
import me.uc.hussein.ultrasclans.event.EventStatus;
import me.uc.hussein.ultrasclans.event.LocationType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class EventRepository {

    private final DatabaseManager db;

    public EventRepository(DatabaseManager db) {
        this.db = db;
    }

    public void insert(ClanEvent event) throws SQLException {
        String sql = """
            INSERT INTO uc_clan_events (id, clan_a, clan_b, status, location_type, round_duration_seconds,
                total_rounds, current_round, score_a, score_b, money_wager, cp_wager, winner_clan_id,
                created_by, created_at, started_at, ended_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, event);
            ps.executeUpdate();
        }
    }

    public void update(ClanEvent event) throws SQLException {
        String sql = """
            UPDATE uc_clan_events SET status = ?, current_round = ?, score_a = ?, score_b = ?,
                winner_clan_id = ?, started_at = ?, ended_at = ?
            WHERE id = ?
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, event.getStatus().name());
            ps.setInt(2, event.getCurrentRound());
            ps.setInt(3, event.getScoreA());
            ps.setInt(4, event.getScoreB());
            ps.setString(5, event.getWinnerClanId() != null ? event.getWinnerClanId().toString() : null);
            setNullableLong(ps, 6, event.getStartedAt());
            setNullableLong(ps, 7, event.getEndedAt());
            ps.setString(8, event.getId().toString());
            ps.executeUpdate();
        }
    }

    public Optional<ClanEvent> findById(UUID id) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_clan_events WHERE id = ?")) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<ClanEvent> findPendingBetween(UUID clanA, UUID clanB) throws SQLException {
        String sql = """
            SELECT * FROM uc_clan_events
            WHERE status = 'PENDING_INVITE' AND ((clan_a = ? AND clan_b = ?) OR (clan_a = ? AND clan_b = ?))
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clanA.toString());
            ps.setString(2, clanB.toString());
            ps.setString(3, clanB.toString());
            ps.setString(4, clanA.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public List<ClanEvent> findActiveOrPendingForClan(UUID clanId) throws SQLException {
        String sql = """
            SELECT * FROM uc_clan_events
            WHERE (clan_a = ? OR clan_b = ?) AND status IN ('PENDING_INVITE', 'ACCEPTED_PREPARING', 'ACTIVE')
            """;
        List<ClanEvent> result = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clanId.toString());
            ps.setString(2, clanId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        return result;
    }

    public List<ClanEvent> findRecentFinishedForClan(UUID clanId, int limit) throws SQLException {
        String sql = """
            SELECT * FROM uc_clan_events WHERE (clan_a = ? OR clan_b = ?) AND status = 'FINISHED'
            ORDER BY ended_at DESC LIMIT ?
            """;
        List<ClanEvent> result = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clanId.toString());
            ps.setString(2, clanId.toString());
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        return result;
    }

    public List<ClanEvent> findAllActiveOrPreparing() throws SQLException {
        String sql = "SELECT * FROM uc_clan_events WHERE status IN ('ACCEPTED_PREPARING', 'ACTIVE')";
        List<ClanEvent> result = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(map(rs));
        }
        return result;
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private void bind(PreparedStatement ps, ClanEvent event) throws SQLException {
        ps.setString(1, event.getId().toString());
        ps.setString(2, event.getClanA().toString());
        ps.setString(3, event.getClanB().toString());
        ps.setString(4, event.getStatus().name());
        ps.setString(5, event.getLocationType().name());
        ps.setInt(6, event.getRoundDurationSeconds());
        ps.setInt(7, event.getTotalRounds());
        ps.setInt(8, event.getCurrentRound());
        ps.setInt(9, event.getScoreA());
        ps.setInt(10, event.getScoreB());
        ps.setDouble(11, event.getMoneyWager());
        ps.setLong(12, event.getCpWager());
        ps.setString(13, event.getWinnerClanId() != null ? event.getWinnerClanId().toString() : null);
        ps.setString(14, event.getCreatedBy() != null ? event.getCreatedBy().toString() : null);
        ps.setLong(15, event.getCreatedAt());
        setNullableLong(ps, 16, event.getStartedAt());
        setNullableLong(ps, 17, event.getEndedAt());
    }

    private ClanEvent map(ResultSet rs) throws SQLException {
        long startedAt = rs.getLong("started_at");
        Long startedAtBoxed = rs.wasNull() ? null : startedAt;
        long endedAt = rs.getLong("ended_at");
        Long endedAtBoxed = rs.wasNull() ? null : endedAt;
        String winnerStr = rs.getString("winner_clan_id");
        String createdByStr = rs.getString("created_by");

        return new ClanEvent(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("clan_a")),
                UUID.fromString(rs.getString("clan_b")),
                EventStatus.valueOf(rs.getString("status")),
                LocationType.valueOf(rs.getString("location_type")),
                rs.getInt("round_duration_seconds"),
                rs.getInt("total_rounds"),
                rs.getInt("current_round"),
                rs.getInt("score_a"),
                rs.getInt("score_b"),
                rs.getDouble("money_wager"),
                rs.getLong("cp_wager"),
                winnerStr != null ? UUID.fromString(winnerStr) : null,
                createdByStr != null ? UUID.fromString(createdByStr) : null,
                rs.getLong("created_at"),
                startedAtBoxed,
                endedAtBoxed
        );
    }
}
