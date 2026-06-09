package ru.gov.servicedesk.model;

/**
 * Комментарий пользователя к заявке.
 */
public class TicketComment {
    private final int id;
    private final int ticketId;
    private final String authorName;
    private final String text;
    private final String createdAt;

    public TicketComment(int id, int ticketId, String authorName, String text, String createdAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.authorName = authorName;
        this.text = text;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getTicketId() {
        return ticketId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getText() {
        return text;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
