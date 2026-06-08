package ru.gov.servicedesk.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WebCategoryRepository {
    private final JdbcTemplate jdbcTemplate;

    public WebCategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findAllNames() {
        return jdbcTemplate.queryForList("SELECT name FROM categories ORDER BY name", String.class);
    }
}
