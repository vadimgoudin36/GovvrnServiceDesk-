package ru.gov.servicedesk.dao;

import ru.gov.servicedesk.db.Database;
import ru.gov.servicedesk.model.ActionLog;
import ru.gov.servicedesk.model.TicketStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActionLogDao {
    public void log(Integer ticketId, Integer userId, String actionType, TicketStatus oldStatus, TicketStatus newStatus, String description) throws SQLException {
        String sql = """
                INSERT INTO action_logs (ticket_id, user_id, action_type, old_status, new_status, description)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (ticketId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, ticketId);
            }
            if (userId == null) {
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setInt(2, userId);
            }
            statement.setString(3, actionType);
            statement.setString(4, oldStatus == null ? null : oldStatus.name());
            statement.setString(5, newStatus == null ? null : newStatus.name());
            statement.setString(6, description);
            statement.executeUpdate();
        }
    }

    public List<ActionLog> findRecent(int limit) throws SQLException {
        String sql = """
                SELECT l.id, l.ticket_id, u.full_name AS user_name, l.action_type,
                       l.old_status, l.new_status, l.description, l.created_at
                FROM action_logs l
                LEFT JOIN users u ON u.id = l.user_id
                ORDER BY l.created_at DESC, l.id DESC
                LIMIT ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ActionLog> logs = new ArrayList<>();
                while (resultSet.next()) {
                    int ticketId = resultSet.getInt("ticket_id");
                    logs.add(new ActionLog(
                            resultSet.getInt("id"),
                            resultSet.wasNull() ? null : ticketId,
                            resultSet.getString("user_name"),
                            resultSet.getString("action_type"),
                            resultSet.getString("old_status"),
                            resultSet.getString("new_status"),
                            resultSet.getString("description"),
                            resultSet.getString("created_at")
                    ));
                }
                return logs;
            }
        }
    }
}
