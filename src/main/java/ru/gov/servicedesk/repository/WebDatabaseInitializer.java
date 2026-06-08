package ru.gov.servicedesk.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class WebDatabaseInitializer {
    private final JdbcTemplate jdbcTemplate;

    public WebDatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    login TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    role TEXT NOT NULL,
                    phone TEXT,
                    max_user_id TEXT,
                    two_factor_enabled INTEGER NOT NULL DEFAULT 0
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS tickets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    category TEXT NOT NULL,
                    status TEXT NOT NULL,
                    priority TEXT NOT NULL DEFAULT 'NORMAL',
                    author_id INTEGER NOT NULL,
                    executor_id INTEGER,
                    due_at TEXT,
                    resolution_report TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (author_id) REFERENCES users(id),
                    FOREIGN KEY (executor_id) REFERENCES users(id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS comments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ticket_id INTEGER NOT NULL,
                    author_id INTEGER NOT NULL,
                    text TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
                    FOREIGN KEY (author_id) REFERENCES users(id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS attachments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ticket_id INTEGER NOT NULL,
                    author_id INTEGER NOT NULL,
                    file_name TEXT NOT NULL,
                    stored_name TEXT NOT NULL,
                    content_type TEXT,
                    size INTEGER NOT NULL,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
                    FOREIGN KEY (author_id) REFERENCES users(id)
                )
                """);

        addColumnIfMissing("users", "phone", "TEXT");
        addColumnIfMissing("users", "max_user_id", "TEXT");
        addColumnIfMissing("users", "two_factor_enabled", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("tickets", "priority", "TEXT NOT NULL DEFAULT 'NORMAL'");
        addColumnIfMissing("tickets", "due_at", "TEXT");
        addColumnIfMissing("tickets", "resolution_report", "TEXT");

        jdbcTemplate.update("""
                INSERT OR IGNORE INTO users (login, password, full_name, role) VALUES
                ('user', '1234', 'Иванов Иван, сотрудник', 'EMPLOYEE'),
                ('it', '1234', 'Петров Петр, IT-специалист', 'SPECIALIST'),
                ('admin', '1234', 'Администратор системы', 'ADMIN')
                """);
        jdbcTemplate.update("""
                INSERT OR IGNORE INTO categories (name) VALUES
                ('Компьютер'), ('Принтер'), ('Сеть'), ('Почта'), ('ПО'), ('Другое')
                """);
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnType) {
        boolean exists = jdbcTemplate.query("PRAGMA table_info(" + tableName + ")",
                rs -> {
                    while (rs.next()) {
                        if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                            return true;
                        }
                    }
                    return false;
                });
        if (!exists) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
        }
    }
}
