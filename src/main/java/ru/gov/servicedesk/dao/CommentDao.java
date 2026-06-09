package ru.gov.servicedesk.dao;

import ru.gov.servicedesk.db.Database;
import ru.gov.servicedesk.model.Role;
import ru.gov.servicedesk.model.TicketComment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Сохраняет и анализирует комментарии к заявкам.
 */
public class CommentDao {
    /**
     * Добавляет комментарий к заявке.
     *
     * @param ticketId идентификатор заявки
     * @param authorId идентификатор автора
     * @param text текст комментария
     * @throws SQLException если сохранение завершилось ошибкой
     */
    public void create(int ticketId, int authorId, String text) throws SQLException {
        String sql = "INSERT INTO comments (ticket_id, author_id, text) VALUES (?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ticketId);
            statement.setInt(2, authorId);
            statement.setString(3, text);
            statement.executeUpdate();
        }
    }

    /**
     * Возвращает комментарии заявки в хронологическом порядке.
     *
     * @param ticketId идентификатор заявки
     * @return список комментариев
     * @throws SQLException если запрос завершился ошибкой
     */
    public List<TicketComment> findByTicket(int ticketId) throws SQLException {
        String sql = """
                SELECT c.id, c.ticket_id, u.full_name AS author_name, c.text, c.created_at
                FROM comments c
                JOIN users u ON u.id = c.author_id
                WHERE c.ticket_id = ?
                ORDER BY c.created_at, c.id
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ticketId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<TicketComment> comments = new ArrayList<>();
                while (resultSet.next()) {
                    comments.add(new TicketComment(
                            resultSet.getInt("id"),
                            resultSet.getInt("ticket_id"),
                            resultSet.getString("author_name"),
                            resultSet.getString("text"),
                            resultSet.getString("created_at")
                    ));
                }
                return comments;
            }
        }
    }

    /**
     * Подсчитывает комментарии заявки.
     *
     * @param ticketId идентификатор заявки
     * @return количество комментариев
     * @throws SQLException если запрос завершился ошибкой
     */
    public int countByTicket(int ticketId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM comments WHERE ticket_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ticketId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("total") : 0;
            }
        }
    }

    /**
     * Проверяет наличие комментария указанной роли с одним из ключевых слов.
     *
     * @param ticketId идентификатор заявки
     * @param role роль автора
     * @param words ключевые слова
     * @return {@code true}, если подходящий комментарий найден
     * @throws SQLException если запрос завершился ошибкой
     */
    public boolean hasCommentByRoleContaining(int ticketId, Role role, String... words) throws SQLException {
        String sql = """
                SELECT c.text
                FROM comments c
                JOIN users u ON u.id = c.author_id
                WHERE c.ticket_id = ? AND u.role = ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ticketId);
            statement.setString(2, role.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    if (containsAny(resultSet.getString("text"), words)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /**
     * Проверяет наличие комментария автора с одним из ключевых слов.
     *
     * @param ticketId идентификатор заявки
     * @param authorId идентификатор автора
     * @param words ключевые слова
     * @return {@code true}, если подходящий комментарий найден
     * @throws SQLException если запрос завершился ошибкой
     */
    public boolean hasCommentByAuthorContaining(int ticketId, int authorId, String... words) throws SQLException {
        String sql = "SELECT text FROM comments WHERE ticket_id = ? AND author_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ticketId);
            statement.setInt(2, authorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    if (containsAny(resultSet.getString("text"), words)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    private boolean containsAny(String text, String... words) {
        if (text == null) {
            return false;
        }
        String lowerText = text.toLowerCase();
        for (String word : words) {
            if (lowerText.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
