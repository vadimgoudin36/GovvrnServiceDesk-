package ru.gov.servicedesk.dao;

import ru.gov.servicedesk.db.Database;
import ru.gov.servicedesk.model.Ticket;
import ru.gov.servicedesk.model.TicketStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Выполняет операции хранения, назначения и изменения статусов заявок.
 *
 * <p>Все значимые ручные и автоматические переходы передаются в
 * {@link ActionLogDao}.</p>
 */
public class TicketDao {
    private final ActionLogDao actionLogDao = new ActionLogDao();

    /**
     * Создает заявку со статусом {@link TicketStatus#NEW}.
     *
     * @param ticket данные новой заявки
     * @return идентификатор созданной заявки
     * @throws SQLException если сохранение завершилось ошибкой
     */
    public int create(Ticket ticket) throws SQLException {
        String sql = """
                INSERT INTO tickets (title, description, category, status, author_id, executor_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, ticket.getTitle());
            statement.setString(2, ticket.getDescription());
            statement.setString(3, ticket.getCategory());
            statement.setString(4, TicketStatus.NEW.name());
            statement.setInt(5, ticket.getAuthorId());
            if (ticket.getExecutorId() == null) {
                statement.setNull(6, java.sql.Types.INTEGER);
            } else {
                statement.setInt(6, ticket.getExecutorId());
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return 0;
        }
    }

    /**
     * Возвращает заявки автора.
     *
     * @param authorId идентификатор автора
     * @return список заявок
     * @throws SQLException если запрос завершился ошибкой
     */
    public List<Ticket> findByAuthor(int authorId) throws SQLException {
        String sql = baseSelect() + " WHERE t.author_id = ? ORDER BY t.updated_at DESC, t.id DESC";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, authorId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapTickets(resultSet);
            }
        }
    }

    /**
     * Возвращает все заявки.
     *
     * @return список заявок
     * @throws SQLException если запрос завершился ошибкой
     */
    public List<Ticket> findAll() throws SQLException {
        String sql = baseSelect() + " ORDER BY t.updated_at DESC, t.id DESC";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapTickets(resultSet);
        }
    }

    /**
     * Ищет заявку по идентификатору.
     *
     * @param ticketId идентификатор заявки
     * @return заявка либо {@code null}
     * @throws SQLException если запрос завершился ошибкой
     */
    public Ticket findById(int ticketId) throws SQLException {
        String sql = baseSelect() + " WHERE t.id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ticketId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Ticket> tickets = mapTickets(resultSet);
                return tickets.isEmpty() ? null : tickets.get(0);
            }
        }
    }

    public void assignTo(int ticketId, int executorId) throws SQLException {
        assignTo(ticketId, executorId, null);
    }

    /**
     * Назначает исполнителя и переводит заявку в работу.
     *
     * @param ticketId идентификатор заявки
     * @param executorId идентификатор исполнителя
     * @param userId пользователь, выполнивший назначение
     * @throws SQLException если обновление завершилось ошибкой
     */
    public void assignTo(int ticketId, int executorId, Integer userId) throws SQLException {
        Ticket ticket = findById(ticketId);
        String sql = """
                UPDATE tickets
                SET executor_id = ?, status = 'IN_PROGRESS', updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, executorId);
            statement.setInt(2, ticketId);
            statement.executeUpdate();
        }
        TicketStatus oldStatus = ticket == null ? null : ticket.getStatus();
        actionLogDao.log(ticketId, userId, "ASSIGN", oldStatus, TicketStatus.IN_PROGRESS, "Заявка назначена исполнителю");
    }

    public void updateStatus(int ticketId, TicketStatus status) throws SQLException {
        updateStatus(ticketId, status, null);
    }

    /**
     * Изменяет статус заявки вручную.
     *
     * @param ticketId идентификатор заявки
     * @param status новый статус
     * @param userId пользователь, изменивший статус
     * @throws SQLException если обновление завершилось ошибкой
     */
    public void updateStatus(int ticketId, TicketStatus status, Integer userId) throws SQLException {
        Ticket ticket = findById(ticketId);
        TicketStatus oldStatus = ticket == null ? null : ticket.getStatus();
        if (oldStatus == status) {
            return;
        }
        String sql = "UPDATE tickets SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setInt(2, ticketId);
            statement.executeUpdate();
        }
        actionLogDao.log(ticketId, userId, "STATUS_CHANGE", oldStatus, status, "Статус изменен пользователем");
    }

    public void completeWithReport(int ticketId, String resolutionReport) throws SQLException {
        completeWithReport(ticketId, resolutionReport, null);
    }

    /**
     * Сохраняет отчет исполнителя и отмечает заявку выполненной.
     *
     * @param ticketId идентификатор заявки
     * @param resolutionReport отчет о выполнении
     * @param userId идентификатор исполнителя
     * @throws SQLException если обновление завершилось ошибкой
     */
    public void completeWithReport(int ticketId, String resolutionReport, Integer userId) throws SQLException {
        Ticket ticket = findById(ticketId);
        TicketStatus oldStatus = ticket == null ? null : ticket.getStatus();
        String sql = """
                UPDATE tickets
                SET status = 'DONE', resolution_report = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, resolutionReport);
            statement.setInt(2, ticketId);
            statement.executeUpdate();
        }
        actionLogDao.log(ticketId, userId, "COMPLETE", oldStatus, TicketStatus.DONE, "IT-специалист оставил отчет о выполнении");
    }

    /**
     * Выполняет автоматический переход и записывает его причину.
     *
     * @param ticketId идентификатор заявки
     * @param status новый статус
     * @param reason описание сработавшего правила
     * @throws SQLException если обновление завершилось ошибкой
     */
    public void updateStatusAutomatically(int ticketId, TicketStatus status, String reason) throws SQLException {
        Ticket ticket = findById(ticketId);
        TicketStatus oldStatus = ticket == null ? null : ticket.getStatus();
        if (oldStatus == status) {
            return;
        }
        String sql = "UPDATE tickets SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setInt(2, ticketId);
            statement.executeUpdate();
        }
        actionLogDao.log(ticketId, null, "AUTO_STATUS_CHANGE", oldStatus, status, reason);
    }

    public Map<String, Integer> countByStatus() throws SQLException {
        return countBy("status");
    }

    public boolean hasStatusChangeLogs(int ticketId) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS total
                FROM action_logs
                WHERE ticket_id = ?
                  AND action_type IN ('STATUS_CHANGE', 'AUTO_STATUS_CHANGE', 'ASSIGN', 'COMPLETE')
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ticketId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt("total") > 0;
            }
        }
    }

    public Map<String, Integer> countByCategory() throws SQLException {
        return countBy("category");
    }

    private Map<String, Integer> countBy(String column) throws SQLException {
        String sql = "SELECT " + column + " AS item, COUNT(*) AS total FROM tickets GROUP BY " + column + " ORDER BY total DESC";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            Map<String, Integer> stats = new LinkedHashMap<>();
            while (resultSet.next()) {
                stats.put(resultSet.getString("item"), resultSet.getInt("total"));
            }
            return stats;
        }
    }

    private String baseSelect() {
        return """
                SELECT t.id, t.title, t.description, t.category, t.status, t.author_id, t.executor_id,
                       t.created_at, t.updated_at, t.resolution_report,
                       author.full_name AS author_name,
                       executor.full_name AS executor_name
                FROM tickets t
                JOIN users author ON author.id = t.author_id
                LEFT JOIN users executor ON executor.id = t.executor_id
                """;
    }

    private List<Ticket> mapTickets(ResultSet resultSet) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        while (resultSet.next()) {
            Ticket ticket = new Ticket();
            ticket.setId(resultSet.getInt("id"));
            ticket.setTitle(resultSet.getString("title"));
            ticket.setDescription(resultSet.getString("description"));
            ticket.setCategory(resultSet.getString("category"));
            ticket.setStatus(TicketStatus.valueOf(resultSet.getString("status")));
            ticket.setAuthorId(resultSet.getInt("author_id"));
            int executorId = resultSet.getInt("executor_id");
            ticket.setExecutorId(resultSet.wasNull() ? null : executorId);
            ticket.setAuthorName(resultSet.getString("author_name"));
            ticket.setExecutorName(resultSet.getString("executor_name"));
            ticket.setCreatedAt(resultSet.getString("created_at"));
            ticket.setUpdatedAt(resultSet.getString("updated_at"));
            ticket.setResolutionReport(resultSet.getString("resolution_report"));
            tickets.add(ticket);
        }
        return tickets;
    }
}
