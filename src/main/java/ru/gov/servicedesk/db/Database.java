package ru.gov.servicedesk.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Управляет подключениями к SQLite и первоначальной структурой базы данных.
 *
 * <p>При первом запуске создает таблицы, категории и тестовых пользователей.</p>
 */
public final class Database {
    private static final Path DB_PATH = appDataDirectory().resolve("servicedesk.db");
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    private Database() {
    }

    /**
     * Открывает соединение с базой данных и включает внешние ключи SQLite.
     *
     * @return открытое JDBC-соединение
     * @throws SQLException если каталог или соединение создать не удалось
     */
    public static Connection getConnection() throws SQLException {
        ensureDatabaseDirectory();
        Connection connection = DriverManager.getConnection(DB_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    /**
     * Создает отсутствующие таблицы и добавляет начальные данные.
     *
     * @throws SQLException если выполнение SQL-команд завершилось ошибкой
     */
    public static void initialize() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            createTables(statement);
            seedData(statement);
        }
    }

    private static void createTables(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    login TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    role TEXT NOT NULL
                )
                """);

        statement.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                )
                """);

        statement.execute("""
                CREATE TABLE IF NOT EXISTS tickets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    category TEXT NOT NULL,
                    status TEXT NOT NULL,
                    author_id INTEGER NOT NULL,
                    executor_id INTEGER,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    resolution_report TEXT,
                    FOREIGN KEY (author_id) REFERENCES users(id),
                    FOREIGN KEY (executor_id) REFERENCES users(id)
                )
                """);

        statement.execute("""
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

        statement.execute("""
                CREATE TABLE IF NOT EXISTS action_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ticket_id INTEGER,
                    user_id INTEGER,
                    action_type TEXT NOT NULL,
                    old_status TEXT,
                    new_status TEXT,
                    description TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
                """);

        addColumnIfMissing(statement, "tickets", "resolution_report", "TEXT");
    }

    private static void seedData(Statement statement) throws SQLException {
        statement.execute("""
                INSERT OR IGNORE INTO users (login, password, full_name, role) VALUES
                ('user', '1234', 'Иванов Иван, сотрудник', 'EMPLOYEE'),
                ('it', '1234', 'Петров Петр, IT-специалист', 'SPECIALIST'),
                ('admin', '1234', 'Администратор системы', 'ADMIN')
                """);

        statement.execute("""
                INSERT OR IGNORE INTO categories (name) VALUES
                ('Компьютер'),
                ('Принтер'),
                ('Сеть'),
                ('Почта'),
                ('ПО'),
                ('Другое')
                """);
    }

    private static void addColumnIfMissing(Statement statement, String tableName, String columnName, String columnType) throws SQLException {
        try (var resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return;
                }
            }
        }
        statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
    }

    private static void ensureDatabaseDirectory() throws SQLException {
        try {
            Files.createDirectories(DB_PATH.getParent());
        } catch (Exception ex) {
            throw new SQLException("Не удалось создать папку для базы данных: " + DB_PATH.getParent(), ex);
        }
    }

    private static Path appDataDirectory() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "ServiceDeskMRB");
        }
        return Paths.get(System.getProperty("user.home"), "ServiceDeskMRB");
    }
}
