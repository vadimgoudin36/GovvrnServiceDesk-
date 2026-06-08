package ru.gov.servicedesk.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.gov.servicedesk.model.TicketComment;

import java.util.List;

@Repository
public class WebCommentRepository {
    private final JdbcTemplate jdbcTemplate;

    public WebCommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(int ticketId, int authorId, String text) {
        jdbcTemplate.update("""
                INSERT INTO comments (ticket_id, author_id, text)
                VALUES (?, ?, ?)
                """, ticketId, authorId, text);
    }

    public List<TicketComment> findByTicket(int ticketId) {
        return jdbcTemplate.query("""
                SELECT c.id, c.ticket_id, u.full_name AS author_name, c.text, c.created_at
                FROM comments c
                JOIN users u ON u.id = c.author_id
                WHERE c.ticket_id = ?
                ORDER BY c.created_at, c.id
                """, (rs, rowNum) -> new TicketComment(
                rs.getInt("id"),
                rs.getInt("ticket_id"),
                rs.getString("author_name"),
                rs.getString("text"),
                rs.getString("created_at")
        ), ticketId);
    }
}
