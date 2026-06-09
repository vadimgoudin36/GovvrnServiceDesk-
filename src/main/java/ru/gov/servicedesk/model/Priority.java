package ru.gov.servicedesk.model;

/**
 * Приоритет заявки.
 */
public enum Priority {
    LOW("Низкий"),
    NORMAL("Обычный"),
    HIGH("Высокий"),
    URGENT("Срочный");

    private final String title;

    Priority(String title) {
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
