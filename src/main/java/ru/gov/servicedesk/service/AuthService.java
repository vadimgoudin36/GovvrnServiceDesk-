package ru.gov.servicedesk.service;

import ru.gov.servicedesk.dao.UserDao;
import ru.gov.servicedesk.model.User;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {
    private final UserDao userDao = new UserDao();

    public Optional<User> login(String login, String password) throws SQLException {
        return userDao.findByLogin(login)
                .filter(user -> user.getPassword().equals(password));
    }
}
