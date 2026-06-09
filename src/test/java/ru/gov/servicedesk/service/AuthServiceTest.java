package ru.gov.servicedesk.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.gov.servicedesk.dao.UserDao;
import ru.gov.servicedesk.model.Role;
import ru.gov.servicedesk.model.User;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты авторизации пользователей.
 */
class AuthServiceTest {
    private UserDao userDao;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDao.class);
        authService = new AuthService(userDao);
    }

    @Test
    @DisplayName("Пользователь входит с правильным паролем")
    void loginReturnsUserForValidCredentials() throws SQLException {
        User user = new User(1, "user", "1234", "Иванов Иван", Role.EMPLOYEE);
        when(userDao.findByLogin("user")).thenReturn(Optional.of(user));

        Optional<User> result = authService.login("user", "1234");

        assertTrue(result.isPresent());
        assertEquals(Role.EMPLOYEE, result.orElseThrow().getRole());
    }

    @Test
    @DisplayName("Неверный пароль отклоняется")
    void loginRejectsInvalidPassword() throws SQLException {
        User user = new User(1, "user", "1234", "Иванов Иван", Role.EMPLOYEE);
        when(userDao.findByLogin("user")).thenReturn(Optional.of(user));

        Optional<User> result = authService.login("user", "wrong-password");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Неизвестный логин отклоняется")
    void loginRejectsUnknownLogin() throws SQLException {
        when(userDao.findByLogin("unknown")).thenReturn(Optional.empty());

        Optional<User> result = authService.login("unknown", "1234");

        assertTrue(result.isEmpty());
    }
}
