package ru.gov.servicedesk.model;

/**
 * Роль пользователя и набор доступных ему функций.
 */
public enum Role {
    EMPLOYEE("Сотрудник"),
    SPECIALIST("IT-специалист"),
    ADMIN("Администратор");

    private final String title;

    Role(String title) {
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
