package ru.gov.servicedesk.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.gov.servicedesk.model.Role;
import ru.gov.servicedesk.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public class WebUserRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> mapper = (rs, rowNum) -> new User(
            rs.getInt("id"),
            rs.getString("login"),
            rs.getString("password"),
            rs.getString("full_name"),
            Role.valueOf(rs.getString("role"))
    );

    public WebUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByLogin(String login) {
        return jdbcTemplate.query("""
                        SELECT id, login, password, full_name, role
                        FROM users
                        WHERE login = ?
                        """, mapper, login)
                .stream()
                .findFirst();
    }

    public Optional<User> findById(int id) {
        return jdbcTemplate.query("""
                        SELECT id, login, password, full_name, role
                        FROM users
                        WHERE id = ?
                        """, mapper, id)
                .stream()
                .findFirst();
    }

    public List<User> findAll() {
        return jdbcTemplate.query("""
                SELECT id, login, password, full_name, role
                FROM users
                ORDER BY full_name
                """, mapper);
    }

    public List<User> findByRole(Role role) {
        return jdbcTemplate.query("""
                SELECT id, login, password, full_name, role
                FROM users
                WHERE role = ?
                ORDER BY full_name
                """, mapper, role.name());
    }

    public void create(String login, String password, String fullName, Role role) {
        jdbcTemplate.update("""
                INSERT INTO users (login, password, full_name, role)
                VALUES (?, ?, ?, ?)
                """, login, password, fullName, role.name());
    }

    public void update(int id, String login, String password, String fullName, Role role) {
        jdbcTemplate.update("""
                UPDATE users
                SET login = ?, password = ?, full_name = ?, role = ?
                WHERE id = ?
                """, login, password, fullName, role.name(), id);
    }
}
