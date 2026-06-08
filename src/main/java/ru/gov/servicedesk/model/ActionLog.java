package ru.gov.servicedesk.model;

public class ActionLog {
    private final int id;
    private final Integer ticketId;
    private final String userName;
    private final String actionType;
    private final String oldStatus;
    private final String newStatus;
    private final String description;
    private final String createdAt;

    public ActionLog(int id, Integer ticketId, String userName, String actionType, String oldStatus, String newStatus, String description, String createdAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.userName = userName;
        this.actionType = actionType;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.description = description;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public Integer getTicketId() {
        return ticketId;
    }

    public String getUserName() {
        return userName;
    }

    public String getActionType() {
        return actionType;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
