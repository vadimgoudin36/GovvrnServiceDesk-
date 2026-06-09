package ru.gov.servicedesk.model;

/**
 * Пользователь ServiceDesk с учетными данными и ролью доступа.
 */
public class User {
    private final int id;
    private final String login;
    private final String password;
    private final String fullName;
    private final Role role;

    /**
     * Создает объект пользователя.
     *
     * @param id идентификатор
     * @param login логин
     * @param password пароль учебной версии
     * @param fullName полное имя
     * @param role роль доступа
     */
    public User(int id, String login, String password, String fullName, Role role) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return fullName + " (" + login + ", " + role + ")";
    }
}
