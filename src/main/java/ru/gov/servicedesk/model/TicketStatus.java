package ru.gov.servicedesk.model;

public enum TicketStatus {
    NEW("Новая"),
    IN_PROGRESS("В работе"),
    DONE("Выполнена"),
    CLOSED("Закрыта"),
    OVERDUE("Просрочена"),
    URGENT_REVIEW("Требует срочного рассмотрения"),
    ESCALATION("Требует эскалации");

    private final String title;

    TicketStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
