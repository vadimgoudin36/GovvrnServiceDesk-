package ru.gov.servicedesk.config;

import org.sqlite.SQLiteDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class DataSourceConfig {
    @Bean
    public DataSource dataSource() throws Exception {
        Path dbPath = appDirectory().resolve("servicedesk-web.db");
        Files.createDirectories(dbPath.getParent());

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dbPath);
        dataSource.setEnforceForeignKeys(true);
        return dataSource;
    }

    @Bean
    public Path uploadDirectory() throws Exception {
        Path uploadDirectory = appDirectory().resolve("uploads");
        Files.createDirectories(uploadDirectory);
        return uploadDirectory;
    }

    private Path appDirectory() {
        String explicitPath = System.getenv("SERVICE_DESK_DATA_DIR");
        if (explicitPath != null && !explicitPath.isBlank()) {
            return Paths.get(explicitPath);
        }
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "ServiceDeskMRB");
        }
        return Paths.get(System.getProperty("user.home"), "ServiceDeskMRB");
    }
}
