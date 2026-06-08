package ru.gov.servicedesk.dao;

import ru.gov.servicedesk.db.Database;
import ru.gov.servicedesk.model.Role;
import ru.gov.servicedesk.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    public Optional<User> findByLogin(String login) throws SQLException {
        String sql = "SELECT id, login, password, full_name, role FROM users WHERE login = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, login, password, full_name, role FROM users ORDER BY id";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
            return users;
        }
    }

    public List<User> findByRole(Role role) throws SQLException {
        String sql = "SELECT id, login, password, full_name, role FROM users WHERE role = ? ORDER BY full_name";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }
                return users;
            }
        }
    }

    public void create(String login, String password, String fullName, Role role) throws SQLException {
        String sql = "INSERT INTO users (login, password, full_name, role) VALUES (?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            statement.setString(2, password);
            statement.setString(3, fullName);
            statement.setString(4, role.name());
            statement.executeUpdate();
        }
    }

    public void update(int id, String login, String password, String fullName, Role role) throws SQLException {
        String sql = "UPDATE users SET login = ?, password = ?, full_name = ?, role = ? WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            statement.setString(2, password);
            statement.setString(3, fullName);
            statement.setString(4, role.name());
            statement.setInt(5, id);
            statement.executeUpdate();
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getInt("id"),
                resultSet.getString("login"),
                resultSet.getString("password"),
                resultSet.getString("full_name"),
                Role.valueOf(resultSet.getString("role"))
        );
    }
}
