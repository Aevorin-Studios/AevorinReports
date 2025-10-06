package dev.aevorinstudios.aevorinReports.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.aevorinstudios.aevorinReports.reports.Report;
import lombok.Getter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    public boolean testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
    private HikariDataSource dataSource;
    @Getter
    private static DatabaseManager instance;

    public DatabaseManager(String host, int port, String database, String username, String password) {
        instance = this;
        initializeDataSource(host, port, database, username, password);
        createTables();
    }

    public DatabaseManager(String filePath) {
        instance = this;
        initializeSQLiteDataSource(filePath);
        createTables();
    }

    public List<Report> getResolvedReportsBefore(LocalDateTime cutoff) {
        String sql = "SELECT * FROM reports WHERE status = 'RESOLVED' AND updated_at < ?"; 
        List<Report> reports = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(cutoff));
            
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(Report.builder()
                        .id(rs.getLong("id"))
                        .reporterUuid(UUID.fromString(rs.getString("reporter_uuid")))
                        .reportedUuid(UUID.fromString(rs.getString("reported_uuid")))
                        .reason(rs.getString("reason"))
                        .serverName(rs.getString("server_name"))
                        .status(Report.ReportStatus.valueOf(rs.getString("status")))
                        .isAnonymous(rs.getBoolean("is_anonymous"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                        .evidenceData(rs.getString("evidence_data"))
                        .coordinates(rs.getString("coordinates"))
                        .world(rs.getString("world"))
                        .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch resolved reports", e);
        }
        
        return reports;
    }

    public List<Report> getRejectedReportsBefore(LocalDateTime cutoff) {
        String sql = "SELECT * FROM reports WHERE status = 'INVALID' AND updated_at < ?";
        List<Report> reports = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(cutoff));
            
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(Report.builder()
                        .id(rs.getLong("id"))
                        .reporterUuid(UUID.fromString(rs.getString("reporter_uuid")))
                        .reportedUuid(UUID.fromString(rs.getString("reported_uuid")))
                        .reason(rs.getString("reason"))
                        .serverName(rs.getString("server_name"))
                        .status(Report.ReportStatus.valueOf(rs.getString("status")))
                        .isAnonymous(rs.getBoolean("is_anonymous"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                        .evidenceData(rs.getString("evidence_data"))
                        .coordinates(rs.getString("coordinates"))
                        .world(rs.getString("world"))
                        .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rejected reports", e);
        }
        
        return reports;
    }

    public void deleteReport(Long id) {
        String sql = "DELETE FROM reports WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete report", e);
        }
    }

    public List<Report> getReportsByStatus(Report.ReportStatus status) {
        String sql = "SELECT * FROM reports WHERE status = ?";
        List<Report> reports = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(Report.builder()
                        .id(rs.getLong("id"))
                        .reporterUuid(UUID.fromString(rs.getString("reporter_uuid")))
                        .reportedUuid(UUID.fromString(rs.getString("reported_uuid")))
                        .reason(rs.getString("reason"))
                        .serverName(rs.getString("server_name"))
                        .status(Report.ReportStatus.valueOf(rs.getString("status")))
                        .isAnonymous(rs.getBoolean("is_anonymous"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                        .evidenceData(rs.getString("evidence_data"))
                        .coordinates(rs.getString("coordinates"))
                        .world(rs.getString("world"))
                        .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reports by status: " + status, e);
        }
        
        return reports;
    }

    public void deleteOldReports(Report.ReportStatus status, LocalDateTime threshold) {
        String sql = "DELETE FROM reports WHERE status = ? AND updated_at < ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setTimestamp(2, Timestamp.valueOf(threshold));
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete old reports with status: " + status, e);
        }
    }

    public List<Report> getActiveReports() {
        return getReportsByStatus(Report.ReportStatus.PENDING);
    }

    public Report getReport(long id) {
        String sql = "SELECT * FROM reports WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Report.builder()
                        .id(rs.getLong("id"))
                        .reporterUuid(UUID.fromString(rs.getString("reporter_uuid")))
                        .reportedUuid(UUID.fromString(rs.getString("reported_uuid")))
                        .reason(rs.getString("reason"))
                        .serverName(rs.getString("server_name"))
                        .status(Report.ReportStatus.valueOf(rs.getString("status")))
                        .isAnonymous(rs.getBoolean("is_anonymous"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                        .evidenceData(rs.getString("evidence_data"))
                        .coordinates(rs.getString("coordinates"))
                        .world(rs.getString("world"))
                        .build();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch report with id: " + id, e);
        }
        
        return null;
    }

    private void initializeSQLiteDataSource(String filePath) {
        try {
            // Create a database directory if it doesn't exist
            java.io.File dbFile = new java.io.File(filePath);
            java.io.File dbDir = dbFile.getParentFile();
            if (dbDir != null && !dbDir.exists()) {
                if (!dbDir.mkdirs()) {
                    throw new RuntimeException("Failed to create database directory: " + dbDir.getAbsolutePath());
                }
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + filePath);
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(1);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            
            dataSource = new HikariDataSource(config);
            
            // Test the connection
            try (Connection conn = dataSource.getConnection()) {
                if (!conn.isValid(5)) {
                    throw new SQLException("Failed to validate database connection");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite database connection", e);
        }
    }

    private void initializeDataSource(String host, int port, String database, String username, String password) {
        int maxRetries = 3;
        int retryDelay = 5000; // 5 seconds
        int attempts = 0;

        while (attempts < maxRetries) {
            try {
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true", host, port, database));
                config.setUsername(username);
                config.setPassword(password);
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(5);
                config.setConnectionTimeout(30000);
                config.setIdleTimeout(600000);
                config.setMaxLifetime(1800000);
                config.setConnectionTestQuery("SELECT 1");
                config.setValidationTimeout(5000);
                config.setInitializationFailTimeout(1);
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useUnicode", "true");
                config.addDataSourceProperty("characterEncoding", "utf8");
                config.addDataSourceProperty("serverTimezone", "UTC");

                dataSource = new HikariDataSource(config);
                
                // Test the connection
                try (Connection conn = dataSource.getConnection()) {
                    if (!conn.isValid(5)) {
                        throw new SQLException("Failed to validate database connection");
                    }
                }
                return; // Successfully connected
            } catch (SQLException e) {
                attempts++;
                if (attempts < maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("Failed to initialize database connection pool after " + maxRetries + " attempts");
    }

    private void createTables() {
        try (Connection conn = getConnection()) {
            // Reports table
            String createReportsTable = dataSource.getJdbcUrl().contains("sqlite") ?
                """
                CREATE TABLE IF NOT EXISTS reports (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    reporter_uuid VARCHAR(36) NOT NULL,
                    reported_uuid VARCHAR(36) NOT NULL,
                    reason TEXT NOT NULL,
                    server_name VARCHAR(64) NOT NULL,
                    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                    is_anonymous BOOLEAN DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    evidence_data TEXT,
                    coordinates VARCHAR(64),
                    world VARCHAR(64)
                )
                """
                : """
                CREATE TABLE IF NOT EXISTS reports (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    reporter_uuid VARCHAR(36) NOT NULL,
                    reported_uuid VARCHAR(36) NOT NULL,
                    reason TEXT NOT NULL,
                    server_name VARCHAR(64) NOT NULL,
                    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                    is_anonymous BOOLEAN DEFAULT 0
                )
                """;

            try (PreparedStatement stmt = conn.prepareStatement(createReportsTable)) {
                stmt.executeUpdate();
            }

            // Report comments table
            String createCommentsTable = dataSource.getJdbcUrl().contains("sqlite") ?
                """
                CREATE TABLE IF NOT EXISTS report_comments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    report_id BIGINT NOT NULL,
                    staff_uuid VARCHAR(36) NOT NULL,
                    comment TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
                )
                """
                : """
                CREATE TABLE IF NOT EXISTS report_comments (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    report_id BIGINT NOT NULL,
                    staff_uuid VARCHAR(36) NOT NULL,
                    comment TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
                )
                """;

            try (PreparedStatement stmt = conn.prepareStatement(createCommentsTable)) {
                stmt.executeUpdate();
            }

            // Report history table for audit logs
            String createHistoryTable = dataSource.getJdbcUrl().contains("sqlite") ?
                """
                CREATE TABLE IF NOT EXISTS report_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    report_id BIGINT NOT NULL,
                    staff_uuid VARCHAR(36) NOT NULL,
                    action VARCHAR(32) NOT NULL,
                    details TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
                )
                """
                : """
                CREATE TABLE IF NOT EXISTS report_history (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    report_id BIGINT NOT NULL,
                    staff_uuid VARCHAR(36) NOT NULL,
                    action VARCHAR(32) NOT NULL,
                    details TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
                )
                """;

            try (PreparedStatement stmt = conn.prepareStatement(createHistoryTable)) {
                stmt.executeUpdate();
            }

            // Server tokens table for proxy-server authentication
            String createTokensTable = dataSource.getJdbcUrl().contains("sqlite") ?
                """
                CREATE TABLE IF NOT EXISTS server_tokens (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    server_name VARCHAR(64) UNIQUE NOT NULL,
                    token VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
                : """
                CREATE TABLE IF NOT EXISTS server_tokens (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    server_name VARCHAR(64) UNIQUE NOT NULL,
                    token VARCHAR(128) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

            try (PreparedStatement stmt = conn.prepareStatement(createTokensTable)) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void updateReport(Report report) {
        String sql = "UPDATE reports SET reporter_uuid = ?, reported_uuid = ?, reason = ?, server_name = ?, status = ?, is_anonymous = ?, updated_at = ?, evidence_data = ?, coordinates = ?, world = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, report.getReporterUuid().toString());
            stmt.setString(2, report.getReportedUuid().toString());
            stmt.setString(3, report.getReason());
            stmt.setString(4, report.getServerName());
            stmt.setString(5, report.getStatus().name());
            stmt.setBoolean(6, report.isAnonymous());
            stmt.setTimestamp(7, Timestamp.valueOf(report.getUpdatedAt()));
            stmt.setString(8, report.getEvidenceData());
            stmt.setString(9, report.getCoordinates());
            stmt.setString(10, report.getWorld());
            stmt.setLong(11, report.getId());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Failed to update report: Report not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update report", e);
        }
    }

    public void saveReport(Report report) {
        String sql = "INSERT INTO reports (reporter_uuid, reported_uuid, reason, server_name, status, is_anonymous, created_at, updated_at, evidence_data, coordinates, world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, report.getReporterUuid().toString());
            stmt.setString(2, report.getReportedUuid().toString());
            stmt.setString(3, report.getReason());
            stmt.setString(4, report.getServerName());
            stmt.setString(5, report.getStatus().name());
            stmt.setBoolean(6, report.isAnonymous());
            stmt.setTimestamp(7, Timestamp.valueOf(report.getCreatedAt()));
            stmt.setTimestamp(8, Timestamp.valueOf(report.getUpdatedAt()));
            stmt.setString(9, report.getEvidenceData());
            stmt.setString(10, report.getCoordinates());
            stmt.setString(11, report.getWorld());
            
            stmt.executeUpdate();
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    report.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save report", e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}