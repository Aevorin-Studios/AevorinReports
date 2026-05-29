package dev.aevorinstudios.aevorinReports.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.aevorinstudios.aevorinReports.reports.Report;
import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private HikariDataSource dataSource;
    private boolean isSqlite;

    @Getter
    private static DatabaseManager instance;

    public DatabaseManager(String host, int port, String database, String username, String password) {
        instance = this;
        this.isSqlite = false;
        initializeDataSource(host, port, database, username, password);
        createTables();
    }

    public DatabaseManager(String filePath) {
        instance = this;
        this.isSqlite = true;
        initializeSQLiteDataSource(filePath);
        createTables();
    }

    // -------------------------------------------------------------------------
    // ResultSet mapping
    // -------------------------------------------------------------------------

    /**
     * Maps the current row of a ResultSet to a Report object.
     * All query methods should use this instead of duplicating field reads.
     */
    private Report mapReport(ResultSet rs) throws SQLException {
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

    private List<Report> queryReports(String sql, StatementConfigurer configurer) {
        List<Report> reports = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            configurer.configure(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapReport(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute query: " + sql, e);
        }
        return reports;
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void configure(PreparedStatement stmt) throws SQLException;
    }

    // -------------------------------------------------------------------------
    // Public query API
    // -------------------------------------------------------------------------

    public boolean testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    public Report getReport(long id) {
        String sql = "SELECT * FROM reports WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapReport(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch report with id: " + id, e);
        }
        return null;
    }

    public List<Report> getReportsByStatus(Report.ReportStatus status) {
        return queryReports(
                "SELECT * FROM reports WHERE status = ?",
                stmt -> stmt.setString(1, status.name()));
    }

    public List<Report> getActiveReports() {
        return getReportsByStatus(Report.ReportStatus.PENDING);
    }

    public List<Report> getReportsByReporter(UUID reporterUuid) {
        return queryReports(
                "SELECT * FROM reports WHERE reporter_uuid = ? ORDER BY created_at DESC",
                stmt -> stmt.setString(1, reporterUuid.toString()));
    }

    public List<Report> getReportsByStatusBefore(Report.ReportStatus status, LocalDateTime cutoff) {
        return queryReports(
                "SELECT * FROM reports WHERE status = ? AND updated_at < ?",
                stmt -> {
                    stmt.setString(1, status.name());
                    stmt.setTimestamp(2, Timestamp.valueOf(cutoff));
                });
    }

    /** @deprecated Use {@link #getReportsByStatusBefore} with {@code ReportStatus.RESOLVED} */
    @Deprecated
    public List<Report> getResolvedReportsBefore(LocalDateTime cutoff) {
        return getReportsByStatusBefore(Report.ReportStatus.RESOLVED, cutoff);
    }

    /** @deprecated Use {@link #getReportsByStatusBefore} with {@code ReportStatus.REJECTED} */
    @Deprecated
    public List<Report> getRejectedReportsBefore(LocalDateTime cutoff) {
        return getReportsByStatusBefore(Report.ReportStatus.REJECTED, cutoff);
    }

    public List<Report> getReportsAfterId(long lastId) {
        return queryReports(
                "SELECT * FROM reports WHERE id > ? ORDER BY id ASC",
                stmt -> stmt.setLong(1, lastId));
    }

    public long getMaxReportId() {
        String sql = "SELECT MAX(id) FROM reports";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            logger.error("Failed to get max report ID: {}", e.getMessage(), e);
        }
        return 0;
    }

    public int getReportCountByStatus(Report.ReportStatus status) {
        String sql = "SELECT COUNT(*) FROM reports WHERE status = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get report count for status {}: {}", status, e.getMessage());
        }
        return 0;
    }

    public int getTotalReportsCount() {
        String sql = "SELECT COUNT(*) FROM reports";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Failed to get total reports count: {}", e.getMessage());
        }
        return 0;
    }

    public int getReportsCountByReporter(UUID reporterUuid) {
        String sql = "SELECT COUNT(*) FROM reports WHERE reporter_uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reporterUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get report count for reporter {}: {}", reporterUuid, e.getMessage());
        }
        return 0;
    }

    public int getReportsCountByReporterAndStatus(UUID reporterUuid, Report.ReportStatus status) {
        String sql = "SELECT COUNT(*) FROM reports WHERE reporter_uuid = ? AND status = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reporterUuid.toString());
            stmt.setString(2, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get report count for reporter {} with status {}: {}", reporterUuid, status, e.getMessage());
        }
        return 0;
    }

    public int getReportsCountByReported(UUID reportedUuid) {
        String sql = "SELECT COUNT(*) FROM reports WHERE reported_uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reportedUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get report count for reported player {}: {}", reportedUuid, e.getMessage());
        }
        return 0;
    }

    public int getReportsCountByReportedAndStatus(UUID reportedUuid, Report.ReportStatus status) {
        String sql = "SELECT COUNT(*) FROM reports WHERE reported_uuid = ? AND status = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reportedUuid.toString());
            stmt.setString(2, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get report count for reported player {} with status {}: {}", reportedUuid, status, e.getMessage());
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

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
                if (generatedKeys.next()) report.setId(generatedKeys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save report", e);
        }
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
                throw new RuntimeException("Failed to update report: Report not found with id " + report.getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update report", e);
        }
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

    // -------------------------------------------------------------------------
    // Connection / schema
    // -------------------------------------------------------------------------

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void initializeSQLiteDataSource(String filePath) {
        try {
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
            try (Connection conn = dataSource.getConnection()) {
                if (!conn.isValid(5)) throw new SQLException("Failed to validate database connection");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite database connection", e);
        }
    }

    private void initializeDataSource(String host, int port, String database, String username, String password) {
        int maxRetries = 3;
        int retryDelay = 5000;

        for (int attempts = 0; attempts < maxRetries; attempts++) {
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
                try (Connection conn = dataSource.getConnection()) {
                    if (!conn.isValid(5)) throw new SQLException("Failed to validate database connection");
                }
                return;
            } catch (SQLException e) {
                if (attempts < maxRetries - 1) {
                    try { Thread.sleep(retryDelay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        throw new RuntimeException("Failed to initialize database connection pool after " + maxRetries + " attempts");
    }

    private void createTables() {
        try (Connection conn = getConnection()) {
            String createReportsTable = isSqlite ? """
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
                    """ : """
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

            String createCommentsTable = isSqlite ? """
                    CREATE TABLE IF NOT EXISTS report_comments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        report_id BIGINT NOT NULL,
                        staff_uuid VARCHAR(36) NOT NULL,
                        comment TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
                    )
                    """ : """
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

            String createHistoryTable = isSqlite ? """
                    CREATE TABLE IF NOT EXISTS report_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        report_id BIGINT NOT NULL,
                        staff_uuid VARCHAR(36) NOT NULL,
                        action VARCHAR(32) NOT NULL,
                        details TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
                    )
                    """ : """
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

            String createTokensTable = isSqlite ? """
                    CREATE TABLE IF NOT EXISTS server_tokens (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        server_name VARCHAR(64) UNIQUE NOT NULL,
                        token VARCHAR(128) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """ : """
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

            ensureTableSchema(conn);
        } catch (SQLException e) {
            logger.error("Failed to create tables: {}", e.getMessage(), e);
        }
    }

    private void ensureTableSchema(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM reports WHERE 1=0");
             ResultSet rs = ps.executeQuery()) {

            java.sql.ResultSetMetaData meta = rs.getMetaData();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                columns.add(meta.getColumnName(i).toLowerCase());
            }

            if (!columns.contains("created_at"))    addColumn(conn, "reports", "created_at",  "TIMESTAMP NULL");
            if (!columns.contains("updated_at"))    addColumn(conn, "reports", "updated_at",  "TIMESTAMP NULL");
            if (!columns.contains("evidence_data")) addColumn(conn, "reports", "evidence_data", "TEXT");
            if (!columns.contains("coordinates"))   addColumn(conn, "reports", "coordinates", "VARCHAR(64)");
            if (!columns.contains("world"))         addColumn(conn, "reports", "world",        "VARCHAR(64)");
            if (!columns.contains("server_name"))   addColumn(conn, "reports", "server_name",  "VARCHAR(64) NOT NULL DEFAULT 'survival'");
            if (!columns.contains("is_anonymous"))  addColumn(conn, "reports", "is_anonymous", "BOOLEAN DEFAULT 0");
        } catch (SQLException e) {
            logger.error("Failed to check/update table schema: {}", e.getMessage(), e);
        }
    }

    private void addColumn(Connection conn, String table, String column, String type) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type)) {
            stmt.executeUpdate();
            logger.info("Added missing column '{}' to table '{}'", column, table);
        }
    }

    // -------------------------------------------------------------------------
    // Server identity
    // -------------------------------------------------------------------------

    public void syncServerIdentity(String token, String currentServerName) {
        String query = "SELECT server_name FROM server_tokens WHERE token = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, token);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String dbServerName = rs.getString("server_name");
                    if (!dbServerName.equals(currentServerName)) {
                        handleServerRename(conn, token, dbServerName, currentServerName);
                    }
                } else {
                    String insert = "INSERT INTO server_tokens (server_name, token) VALUES (?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                        insertStmt.setString(1, currentServerName);
                        insertStmt.setString(2, token);
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE") || e.getMessage().contains("unique")) {
                forceUpdateTokenForServer(currentServerName, token);
            } else {
                logger.error("Failed to sync server identity: {}", e.getMessage(), e);
            }
        }
    }

    private void handleServerRename(Connection conn, String token, String oldName, String newName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE server_tokens SET server_name = ? WHERE token = ?")) {
            ps.setString(1, newName);
            ps.setString(2, token);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("UPDATE reports SET server_name = ? WHERE server_name = ?")) {
            ps.setString(1, newName);
            ps.setString(2, oldName);
            ps.executeUpdate();
        }
        logger.info("Server renamed from {} to {}. Historic reports updated.", oldName, newName);
    }

    private boolean isServerNameTaken(Connection conn, String serverName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM server_tokens WHERE server_name = ?")) {
            ps.setString(1, serverName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void forceUpdateTokenForServer(String serverName, String newToken) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE server_tokens SET token = ? WHERE server_name = ?")) {
            ps.setString(1, newToken);
            ps.setString(2, serverName);
            ps.executeUpdate();
            logger.info("Re-registered server '{}' with new token.", serverName);
        } catch (SQLException ex) {
            logger.error("Failed to force update token for server {}: {}", serverName, ex.getMessage(), ex);
        }
    }

    public boolean hasMultipleServers() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(DISTINCT server_name) FROM server_tokens");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1) > 1;
        } catch (SQLException e) {
            logger.error("Failed to check for multiple servers: {}", e.getMessage(), e);
        }
        return false;
    }
}
