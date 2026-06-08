package ru.gov.servicedesk.dao;

import ru.gov.servicedesk.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {
    public List<String> findAllNames() throws SQLException {
        String sql = "SELECT name FROM categories ORDER BY name";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<String> categories = new ArrayList<>();
            while (resultSet.next()) {
                categories.add(resultSet.getString("name"));
            }
            return categories;
        }
    }
}
