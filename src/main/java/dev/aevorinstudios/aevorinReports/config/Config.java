package dev.aevorinstudios.aevorinReports.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Data
public class Config {
    private AuthConfig auth = new AuthConfig();
    private DatabaseConfig database = new DatabaseConfig();
    private ReportsConfig reports = new ReportsConfig();
    private SecurityConfig security = new SecurityConfig();
    private NotificationsConfig notifications = new NotificationsConfig();
    private PerformanceConfig performance = new PerformanceConfig();
    private Map<String, String> customReasons = new HashMap<>();

    @Data
    public static class AuthConfig {
        private String token = "";
        private boolean regenerateToken = false;
    }

    @Data
    public static class DatabaseConfig {
        private String type = "file";
        private MySQLConfig mysql = new MySQLConfig();
        private FileConfig file = new FileConfig();
    }

    @Data
    public static class MySQLConfig {
        private String host = "localhost";
        private int port = 3306;
        private String database = "aevorinreports";
        private String username = "root";
        private String password = "";
    }

    @Data
    public static class FileConfig {
        private String path = "database/reports.db";
    }

    @Data
    public static class ReportsConfig {
        private int cooldownSeconds = 300;
        private boolean allowAnonymousReports = true;
        private int maxActiveReportsPerPlayer = 3;
        private int chatHistoryLines = 50;
        private boolean logInventory = true;
        private boolean logLocation = true;
    }

    @Data
    public static class SecurityConfig {
        private int maxFalseReports = 5;
        private int falseReportBanHours = 24;
        private int maxReportsBeforeFlag = 10;
        private String encryptionKey = "";
        private boolean enableTokenAuth = true;
    }

    @Data
    public static class NotificationsConfig {
        private boolean enableChatNotifications = true;
        private boolean enableTitleNotifications = true;
        private boolean enableSoundNotifications = true;
        private String notificationSound = "BLOCK_NOTE_BLOCK_PLING";
    }

    @Data
    public static class PerformanceConfig {
        private boolean enableCaching = true;
        private int cacheDuration = 15;
        private int maxCacheSize = 1000;
        private boolean asyncProcessing = true;
        private int batchSize = 50;
        private int backgroundTaskInterval = 300;
        private int cacheCleanupInterval = 30;
    }
}