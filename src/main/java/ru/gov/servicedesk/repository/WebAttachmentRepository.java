package ru.gov.servicedesk.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.gov.servicedesk.model.Attachment;

import java.util.List;
import java.util.Optional;

@Repository
public class WebAttachmentRepository {
    private final JdbcTemplate jdbcTemplate;

    public WebAttachmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(int ticketId, int authorId, String fileName, String storedName, String contentType, long size) {
        jdbcTemplate.update("""
                INSERT INTO attachments (ticket_id, author_id, file_name, stored_name, content_type, size)
                VALUES (?, ?, ?, ?, ?, ?)
                """, ticketId, authorId, fileName, storedName, contentType, size);
    }

    public List<Attachment> findByTicket(int ticketId) {
        return jdbcTemplate.query("""
                SELECT a.id, a.ticket_id, a.file_name, a.stored_name, a.content_type, a.size, a.created_at,
                       u.full_name AS author_name
                FROM attachments a
                JOIN users u ON u.id = a.author_id
                WHERE a.ticket_id = ?
                ORDER BY a.created_at, a.id
                """, (rs, rowNum) -> new Attachment(
                rs.getInt("id"),
                rs.getInt("ticket_id"),
                rs.getString("file_name"),
                rs.getString("stored_name"),
                rs.getString("content_type"),
                rs.getLong("size"),
                rs.getString("created_at"),
                rs.getString("author_name")
        ), ticketId);
    }

    public Optional<Attachment> findById(int id) {
        return jdbcTemplate.query("""
                SELECT a.id, a.ticket_id, a.file_name, a.stored_name, a.content_type, a.size, a.created_at,
                       u.full_name AS author_name
                FROM attachments a
                JOIN users u ON u.id = a.author_id
                WHERE a.id = ?
                """, (rs, rowNum) -> new Attachment(
                rs.getInt("id"),
                rs.getInt("ticket_id"),
                rs.getString("file_name"),
                rs.getString("stored_name"),
                rs.getString("content_type"),
                rs.getLong("size"),
                rs.getString("created_at"),
                rs.getString("author_name")
        ), id).stream().findFirst();
    }
}
