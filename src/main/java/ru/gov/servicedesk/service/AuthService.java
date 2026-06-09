package ru.gov.servicedesk.service;

import ru.gov.servicedesk.dao.UserDao;
import ru.gov.servicedesk.model.User;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Выполняет авторизацию пользователя по логину и паролю.
 *
 * <p>Хранение пароля открытым текстом используется только в учебной версии.</p>
 */
public class AuthService {
    private final UserDao userDao;

    /**
     * Создает сервис, использующий стандартный DAO пользователей.
     */
    public AuthService() {
        this(new UserDao());
    }

    /**
     * Создает сервис с заданным DAO.
     *
     * <p>Конструктор используется для изолированного unit-тестирования.</p>
     *
     * @param userDao источник данных пользователей
     */
    AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Проверяет учетные данные пользователя.
     *
     * @param login логин
     * @param password пароль
     * @return найденный пользователь либо пустой результат
     * @throws SQLException если чтение пользователя из базы завершилось ошибкой
     */
    public Optional<User> login(String login, String password) throws SQLException {
        return userDao.findByLogin(login)
                .filter(user -> user.getPassword().equals(password));
    }
}
