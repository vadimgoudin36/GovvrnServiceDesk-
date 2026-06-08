package ru.gov.servicedesk.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.gov.servicedesk.model.Priority;
import ru.gov.servicedesk.model.Ticket;
import ru.gov.servicedesk.model.TicketStatus;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class WebTicketRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Ticket> mapper = (rs, rowNum) -> {
        Ticket ticket = new Ticket();
        ticket.setId(rs.getInt("id"));
        ticket.setTitle(rs.getString("title"));
        ticket.setDescription(rs.getString("description"));
        ticket.setCategory(rs.getString("category"));
        ticket.setStatus(TicketStatus.valueOf(rs.getString("status")));
        ticket.setPriority(Priority.valueOf(rs.getString("priority")));
        ticket.setAuthorId(rs.getInt("author_id"));
        int executorId = rs.getInt("executor_id");
        ticket.setExecutorId(rs.wasNull() ? null : executorId);
        ticket.setAuthorName(rs.getString("author_name"));
        ticket.setExecutorName(rs.getString("executor_name"));
        ticket.setDueAt(rs.getString("due_at"));
        ticket.setResolutionReport(rs.getString("resolution_report"));
        ticket.setCreatedAt(rs.getString("created_at"));
        ticket.setUpdatedAt(rs.getString("updated_at"));
        return ticket;
    };

    public WebTicketRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int create(String title, String description, String category, Priority priority, int authorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO tickets (title, description, category, status, priority, author_id)
                    VALUES (?, ?, ?, 'NEW', ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, title);
            statement.setString(2, description);
            statement.setString(3, category);
            statement.setString(4, priority.name());
            statement.setInt(5, authorId);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().intValue();
    }

    public List<Ticket> findAll() {
        return jdbcTemplate.query(baseSelect() + " ORDER BY t.updated_at DESC, t.id DESC", mapper);
    }

    public List<Ticket> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String pattern = "%" + query.trim().toLowerCase() + "%";
        return jdbcTemplate.query(baseSelect() + """
                WHERE CAST(t.id AS TEXT) LIKE ?
                   OR LOWER(t.title) LIKE ?
                   OR LOWER(t.description) LIKE ?
                   OR LOWER(t.category) LIKE ?
                   OR LOWER(t.status) LIKE ?
                   OR LOWER(t.priority) LIKE ?
                   OR LOWER(author.full_name) LIKE ?
                   OR LOWER(COALESCE(executor.full_name, '')) LIKE ?
                ORDER BY t.updated_at DESC, t.id DESC
                """, mapper, pattern, pattern, pattern, pattern, pattern, pattern, pattern, pattern);
    }

    public List<Ticket> findByAuthor(int authorId) {
        return jdbcTemplate.query(baseSelect() + " WHERE t.author_id = ? ORDER BY t.updated_at DESC, t.id DESC", mapper, authorId);
    }

    public List<Ticket> findByExecutor(int executorId) {
        return jdbcTemplate.query(baseSelect() + " WHERE t.executor_id = ? ORDER BY t.updated_at DESC, t.id DESC", mapper, executorId);
    }

    public Optional<Ticket> findById(int id) {
        return jdbcTemplate.query(baseSelect() + " WHERE t.id = ?", mapper, id)
                .stream()
                .findFirst();
    }

    public void assign(int ticketId, int executorId, String dueAt, Priority priority) {
        jdbcTemplate.update("""
                UPDATE tickets
                SET executor_id = ?, due_at = ?, priority = ?, status = 'IN_PROGRESS', updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, executorId, blankToNull(dueAt), priority.name(), ticketId);
    }

    public void takeToWork(int ticketId, int executorId) {
        jdbcTemplate.update("""
                UPDATE tickets
                SET executor_id = ?, status = 'IN_PROGRESS', updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, executorId, ticketId);
    }

    public void updateStatus(int ticketId, TicketStatus status) {
        jdbcTemplate.update("""
                UPDATE tickets
                SET status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, status.name(), ticketId);
    }

    public void completeWithReport(int ticketId, String report) {
        jdbcTemplate.update("""
                UPDATE tickets
                SET status = 'DONE', resolution_report = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, report, ticketId);
    }

    public Map<String, Integer> countByStatus() {
        return countBy("status");
    }

    public Map<String, Integer> countByPriority() {
        return countBy("priority");
    }

    private Map<String, Integer> countBy(String column) {
        Map<String, Integer> result = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT " + column + " AS item, COUNT(*) AS total FROM tickets GROUP BY " + column + " ORDER BY total DESC",
                (RowCallbackHandler) rs -> result.put(rs.getString("item"), rs.getInt("total")));
        return result;
    }

    private String baseSelect() {
        return """
                SELECT t.id, t.title, t.description, t.category, t.status, t.priority,
                       t.author_id, t.executor_id, t.due_at, t.resolution_report,
                       t.created_at, t.updated_at,
                       author.full_name AS author_name,
                       executor.full_name AS executor_name
                FROM tickets t
                JOIN users author ON author.id = t.author_id
                LEFT JOIN users executor ON executor.id = t.executor_id
                """;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
