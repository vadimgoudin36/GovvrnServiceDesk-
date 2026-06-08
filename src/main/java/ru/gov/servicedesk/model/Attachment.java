package ru.gov.servicedesk.model;

public class Attachment {
    private final int id;
    private final int ticketId;
    private final String fileName;
    private final String storedName;
    private final String contentType;
    private final long size;
    private final String createdAt;
    private final String authorName;

    public Attachment(int id, int ticketId, String fileName, String storedName, String contentType, long size, String createdAt, String authorName) {
        this.id = id;
        this.ticketId = ticketId;
        this.fileName = fileName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.size = size;
        this.createdAt = createdAt;
        this.authorName = authorName;
    }

    public int getId() {
        return id;
    }

    public int getTicketId() {
        return ticketId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStoredName() {
        return storedName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getAuthorName() {
        return authorName;
    }
}
