package me.uc.hussein.ultrasclans.repository;

import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.model.ClanTaskProgress;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TaskRepository {

    public record Row(UUID clanId, ClanTaskProgress.Period period, ClanTaskProgress progress) {
    }

    private final DatabaseManager db;

    public TaskRepository(DatabaseManager db) {
        this.db = db;
    }

    public List<Row> findAll() throws SQLException {
        List<Row> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM uc_clan_tasks");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    public void save(UUID clanId, ClanTaskProgress.Period period, ClanTaskProgress progress) throws SQLException {
        try (Connection conn = db.getConnection()) {
            boolean exists;
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT 1 FROM uc_clan_tasks WHERE clan_id = ? AND period = ?")) {
                check.setString(1, clanId.toString());
                check.setString(2, period.name());
                try (ResultSet rs = check.executeQuery()) {
                    exists = rs.next();
                }
            }
            String sql = exists
                    ? "UPDATE uc_clan_tasks SET period_key = ?, progress = ?, claimed = ? WHERE clan_id = ? AND period = ?"
                    : "INSERT INTO uc_clan_tasks (period_key, progress, claimed, clan_id, period) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, progress.getPeriodKey());
                ps.setLong(2, progress.getProgress());
                ps.setInt(3, progress.isClaimed() ? 1 : 0);
                ps.setString(4, clanId.toString());
                ps.setString(5, period.name());
                ps.executeUpdate();
            }
        }
    }

    public void deleteByClan(UUID clanId) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM uc_clan_tasks WHERE clan_id = ?")) {
            ps.setString(1, clanId.toString());
            ps.executeUpdate();
        }
    }

    private Row map(ResultSet rs) throws SQLException {
        return new Row(
                UUID.fromString(rs.getString("clan_id")),
                ClanTaskProgress.Period.valueOf(rs.getString("period")),
                new ClanTaskProgress(rs.getLong("period_key"), rs.getLong("progress"), rs.getInt("claimed") == 1)
        );
    }
}
