package dev.aevorinstudios.aevorinReports.config;

import lombok.Data;
import lombok.Getter;
import org.bukkit.Sound;

@Data
public class Settings {
    private ProxySettings proxy = new ProxySettings();
    private DatabaseSettings database = new DatabaseSettings();
    private ReportSettings reports = new ReportSettings();
    private NotificationSettings notifications = new NotificationSettings();
    private PerformanceSettings performance = new PerformanceSettings();
    private StorageSettings storage = new StorageSettings();
    private DebugSettings debug = new DebugSettings();
    private MessageSettings messages = new MessageSettings();

    @Data
    public static class ProxySettings {
        private boolean enabled = false;
        private String token = "";
    }

    @Data
    public static class DatabaseSettings {
        private String type = "mysql";
        private String host = "localhost";
        private int port = 3306;
        private String database = "aevorin_reports";
        private String username = "root";
        private String password = "password";
        private PoolSettings pool = new PoolSettings();

        @Data
        public static class PoolSettings {
            private int minimumIdle = 5;
            private int maximumPoolSize = 10;
            private int connectionTimeout = 30000;
        }
    }

    @Data
    public static class ReportSettings {
        private int cooldown = 300;
        private int maxActiveReports = 3;
        private String[] categories = {"Hacking", "Griefing", "Chat Abuse", "Other"};
        private int minDescriptionLength = 10;
        private int maxDescriptionLength = 500;
    }

    @Data
    public static class NotificationSettings {
        private boolean newReport = true;
        private boolean statusChange = true;
        private String sound = "BLOCK_NOTE_BLOCK_PLING";
        private String prefix = "&8[&bAevorinReports&8]&r ";

        public Sound getSound() {
            try {
                return Sound.valueOf(sound.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Sound.BLOCK_NOTE_BLOCK_PLING;
            }
        }
    }

    @Data
    public static class PerformanceSettings {
        private boolean enableCaching = true;
        private int cacheDuration = 15;
        private int maxCacheSize = 1000;
        private boolean asyncProcessing = true;
        private int batchSize = 50;
        private int backgroundTaskInterval = 300;
        private int cacheCleanupInterval = 30;
    }

    @Data
    public static class StorageSettings {
        private int keepResolvedReports = 30;
        private int keepRejectedReports = 7;
    }

    @Data
    public static class DebugSettings {
        private boolean enabled = false;
        private boolean logQueries = false;
    }

    @Data
    public static class MessageSettings {
        private String reportCreated = "&aYour report has been submitted successfully!";
        private String reportCooldown = "&cYou must wait {time} before submitting another report.";
        private String reportLimitReached = "&cYou have reached the maximum number of active reports.";
        private String noPermission = "&cYou don't have permission to do that!";
        private String invalidPlayer = "&cThat player does not exist!";
        private String descriptionTooShort = "&cYour report description is too short! Minimum length: {min}";
        private String descriptionTooLong = "&cYour report description is too long! Maximum length: {max}";
        private String invalidCategory = "&cInvalid report category! Available categories: {categories}";
        private String reportNotification = "&b{reporter} &7has reported &b{reported} &7for &b{category}&7.";
        private String reportStatusChange = "&7Report #{id} status has been changed to &b{status}&7.";
    }
}