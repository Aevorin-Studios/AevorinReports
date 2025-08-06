package dev.aevorinstudios.aevorinReports.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Data
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static volatile ConfigManager instance;
    private static final ReentrantReadWriteLock configLock = new ReentrantReadWriteLock();
    
    private final Path configPath;
    private Config config;

    private ConfigManager(Path dataDirectory) {
        this.configPath = dataDirectory.resolve("config.yml");
        loadConfig();
    }
    
    public static ConfigManager initialize(Path dataDirectory) {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager(dataDirectory);
                }
            }
        }
        return instance;
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ConfigManager has not been initialized. Call initialize() first.");
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private Config convertYamlToConfig(Map<String, Object> yamlConfig) {
        Config config = new Config();
        
        if (yamlConfig == null) return config;

        // Auth Configuration
        if (yamlConfig.containsKey("auth")) {
            Map<?, ?> authRaw = (Map<?, ?>) yamlConfig.get("auth");
            Map<String, Object> auth = new HashMap<>();
            if (authRaw != null) {
                for (Map.Entry<?, ?> entry : authRaw.entrySet()) {
                    auth.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            if (!auth.isEmpty()) {
                config.getAuth().setToken((String) auth.getOrDefault("token", ""));
                config.getAuth().setRegenerateToken((Boolean) auth.getOrDefault("regenerateToken", false));
            }
        }

        // Database Configuration
        if (yamlConfig.containsKey("database")) {
            Map<String, Object> db = (Map<String, Object>) yamlConfig.get("database");
            if (db != null) {
                config.getDatabase().setType((String) db.getOrDefault("type", "file"));
                
                // MySQL Config
                if (db.containsKey("mysql")) {
                    Map<String, Object> mysql = (Map<String, Object>) db.get("mysql");
                    if (mysql != null) {
                        config.getDatabase().getMysql().setHost((String) mysql.getOrDefault("host", "localhost"));
                        config.getDatabase().getMysql().setPort(Integer.parseInt(String.valueOf(mysql.getOrDefault("port", 3306))));
                        config.getDatabase().getMysql().setDatabase((String) mysql.getOrDefault("database", "aevorinreports"));
                        config.getDatabase().getMysql().setUsername((String) mysql.getOrDefault("username", "root"));
                        config.getDatabase().getMysql().setPassword((String) mysql.getOrDefault("password", ""));
                    }
                }

                // File Storage Config
                if (db.containsKey("file")) {
                    Map<String, Object> file = (Map<String, Object>) db.get("file");
                    if (file != null) {
                        config.getDatabase().getFile().setPath((String) file.getOrDefault("path", "database/reports.db"));
                    }
                }
            }
        }

        // Reports Configuration
        if (yamlConfig.containsKey("reports")) {
            Map<String, Object> reports = (Map<String, Object>) yamlConfig.get("reports");
            if (reports != null) {
                config.getReports().setCooldownSeconds((Integer) reports.getOrDefault("cooldownSeconds", 300));
                config.getReports().setAllowAnonymousReports((Boolean) reports.getOrDefault("allowAnonymousReports", true));
                config.getReports().setMaxActiveReportsPerPlayer((Integer) reports.getOrDefault("maxActiveReportsPerPlayer", 3));
                config.getReports().setChatHistoryLines((Integer) reports.getOrDefault("chatHistoryLines", 50));
                config.getReports().setLogInventory((Boolean) reports.getOrDefault("logInventory", true));
                config.getReports().setLogLocation((Boolean) reports.getOrDefault("logLocation", true));
            }
        }

        // Security Configuration
        if (yamlConfig.containsKey("security")) {
            Map<String, Object> security = (Map<String, Object>) yamlConfig.get("security");
            if (security != null) {
                config.getSecurity().setMaxFalseReports(Integer.parseInt(String.valueOf(security.getOrDefault("maxFalseReports", 5))));
                config.getSecurity().setFalseReportBanHours((Integer) security.getOrDefault("falseReportBanHours", 24));
                config.getSecurity().setMaxReportsBeforeFlag((Integer) security.getOrDefault("maxReportsBeforeFlag", 10));
                config.getSecurity().setEncryptionKey((String) security.getOrDefault("encryptionKey", ""));
                config.getSecurity().setEnableTokenAuth((Boolean) security.getOrDefault("enableTokenAuth", true));
            }
        }

        // Notifications Configuration
        if (yamlConfig.containsKey("notifications")) {
            Map<String, Object> notifications = (Map<String, Object>) yamlConfig.get("notifications");
            if (notifications != null) {
                config.getNotifications().setEnableChatNotifications((Boolean) notifications.getOrDefault("enableChatNotifications", true));
                config.getNotifications().setEnableTitleNotifications((Boolean) notifications.getOrDefault("enableTitleNotifications", true));
                config.getNotifications().setEnableSoundNotifications((Boolean) notifications.getOrDefault("enableSoundNotifications", true));
                config.getNotifications().setNotificationSound((String) notifications.getOrDefault("notificationSound", "BLOCK_NOTE_BLOCK_PLING"));
            }
        }

        // Performance Configuration
        if (yamlConfig.containsKey("performance")) {
            Map<String, Object> performance = (Map<String, Object>) yamlConfig.get("performance");
            if (performance != null) {
                config.getPerformance().setEnableCaching((Boolean) performance.getOrDefault("enableCaching", true));
                config.getPerformance().setCacheDuration((Integer) performance.getOrDefault("cacheDuration", 15));
                config.getPerformance().setMaxCacheSize((Integer) performance.getOrDefault("maxCacheSize", 1000));
                config.getPerformance().setAsyncProcessing((Boolean) performance.getOrDefault("asyncProcessing", true));
                config.getPerformance().setBatchSize((Integer) performance.getOrDefault("batchSize", 50));
                config.getPerformance().setBackgroundTaskInterval((Integer) performance.getOrDefault("backgroundTaskInterval", 300));
                config.getPerformance().setCacheCleanupInterval((Integer) performance.getOrDefault("cacheCleanupInterval", 30));
            }
        }

        // Custom Reasons
        if (yamlConfig.containsKey("customReasons")) {
            Map<?, ?> customReasonsRaw = (Map<?, ?>) yamlConfig.get("customReasons");
            Map<String, String> customReasons = new HashMap<>();
            if (customReasonsRaw != null) {
                for (Map.Entry<?, ?> entry : customReasonsRaw.entrySet()) {
                    customReasons.put(
                        String.valueOf(entry.getKey()), 
                        String.valueOf(entry.getValue())
                    );
                }
                config.setCustomReasons(new HashMap<>(customReasons));
            }
        }

        return config;
    }

    public void loadConfig() {
        configLock.writeLock().lock();
        try {
            if (!Files.exists(configPath)) {
                logger.info("Configuration file not found, creating default config.yml");
                config = createDefaultConfig();
                saveConfig();
                return;
            }
            
            logger.info("Loading configuration from {}", configPath);
            try (InputStream inputStream = Files.newInputStream(configPath)) {
                LoaderOptions loaderOptions = new LoaderOptions();
                loaderOptions.setAllowDuplicateKeys(false);
                loaderOptions.setMaxAliasesForCollections(50);
                
                Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
                Object loadedYaml = yaml.load(inputStream);
                
                if (!(loadedYaml instanceof Map)) {
                    throw new IllegalStateException("Invalid configuration format: Root element must be a mapping");
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> yamlConfig = (Map<String, Object>) loadedYaml;
                
                try {
                    config = convertYamlToConfig(yamlConfig);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to convert configuration: " + e.getMessage(), e);
                }
                
                try {
                    validateConfiguration();
                } catch (Exception e) {
                    throw new IllegalStateException("Configuration validation failed: " + e.getMessage(), e);
                }
                
                if (config.getSecurity().getEncryptionKey().isEmpty()) {
                    config.getSecurity().setEncryptionKey(generateSecureKey());
                    saveConfig();
                }
                
                logger.info("Configuration loaded successfully");
                
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read configuration file: " + e.getMessage(), e);
            }
        } catch (IllegalStateException e) {
            logger.error("Configuration error: {}", e.getMessage(), e);
            logger.warn("Loading default configuration");
            config = createDefaultConfig();
            config.getSecurity().setEncryptionKey(generateSecureKey());
            try {
                saveConfig();
            } catch (Exception ex) {
                logger.error("Failed to save default configuration: {}", ex.getMessage(), ex);
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    private String generateSecureKey() {
        // Generate a secure random key that is guaranteed to be at least 32 characters
        byte[] key = new byte[48]; // Using 48 bytes to ensure the Base64 encoding is over 32 chars
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
    
    public Config getConfig() {
        configLock.readLock().lock();
        try {
            return config;
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    private Config createDefaultConfig() {
        Config defaultConfig = new Config();
        defaultConfig.getSecurity().setEncryptionKey(generateSecureKey());
        return defaultConfig;
    }
    
    private void validateConfiguration() throws IllegalStateException {
        if (config == null) {
            throw new IllegalStateException("Configuration not loaded");
        }
        
        // Check and fix encryption key if needed
        if (config.getSecurity().getEncryptionKey() == null || 
            config.getSecurity().getEncryptionKey().isEmpty() || 
            config.getSecurity().getEncryptionKey().length() < 32) {
            logger.warn("Encryption key missing or too short, generating a new secure key");
            config.getSecurity().setEncryptionKey(generateSecureKey());
            // Don't throw an exception here, just fix it
        }
        
        if (config.getDatabase().getType().equals("mysql") && 
            (config.getDatabase().getMysql().getPassword().isEmpty() || 
             config.getDatabase().getMysql().getUsername().isEmpty())) {
            throw new IllegalStateException("MySQL credentials must be provided");
        }
        
        validateDatabaseConfig();
        validateSecurityConfig();
        validatePerformanceConfig();
        validateReportsConfig();
    }
    
    private void validateDatabaseConfig() {
        Config.DatabaseConfig db = config.getDatabase();
        if (!"mysql".equals(db.getType()) && !"file".equals(db.getType())) {
            logger.warn("Invalid database type '{}', defaulting to 'file'", db.getType());
            db.setType("file");
        }

        if ("mysql".equals(db.getType())) {
            // Correct class path: MySQLConfig is nested inside DatabaseConfig
            Config.DatabaseConfig.MySQLConfig mysql = db.getMysql();
            if (mysql.getPort() <= 0 || mysql.getPort() > 65535) {
                logger.warn("Invalid MySQL port {}, defaulting to 3306", mysql.getPort());
                mysql.setPort(3306);
            }
        }
    }
    
    private void validateSecurityConfig() {
        Config.SecurityConfig security = config.getSecurity();
        if (security.getMaxFalseReports() < 0) {
            logger.warn("Invalid maxFalseReports value, setting to default");
            security.setMaxFalseReports(5);
        }
        if (security.getFalseReportBanHours() < 0) {
            logger.warn("Invalid falseReportBanHours value, setting to default");
            security.setFalseReportBanHours(24);
        }
        if (security.getMaxReportsBeforeFlag() < 0) {
            logger.warn("Invalid maxReportsBeforeFlag value, setting to default");
            security.setMaxReportsBeforeFlag(10);
        }
    }
    
    private void validatePerformanceConfig() {
        Config.PerformanceConfig performance = config.getPerformance();
        if (performance.getCacheDuration() < 0) {
            logger.warn("Invalid cacheDuration value, setting to default");
            performance.setCacheDuration(15);
        }
        if (performance.getMaxCacheSize() < 0) {
            logger.warn("Invalid maxCacheSize value, setting to default");
            performance.setMaxCacheSize(1000);
        }
        if (performance.getBatchSize() < 1) {
            logger.warn("Invalid batchSize value, setting to default");
            performance.setBatchSize(50);
        }
        if (performance.getBackgroundTaskInterval() < 0) {
            logger.warn("Invalid backgroundTaskInterval value, setting to default");
            performance.setBackgroundTaskInterval(300);
        }
        if (performance.getCacheCleanupInterval() < 0) {
            logger.warn("Invalid cacheCleanupInterval value, setting to default");
            performance.setCacheCleanupInterval(30);
        }
    }
    
    private void validateReportsConfig() {
        Config.ReportsConfig reports = config.getReports();
        if (reports.getCooldownSeconds() < 0) {
            logger.warn("Invalid cooldownSeconds value, setting to default");
            reports.setCooldownSeconds(300);
        }
        if (reports.getMaxActiveReportsPerPlayer() < 1) {
            logger.warn("Invalid maxActiveReportsPerPlayer value, setting to default");
            reports.setMaxActiveReportsPerPlayer(3);
        }
        if (reports.getChatHistoryLines() < 0) {
            logger.warn("Invalid chatHistoryLines value, setting to default");
            reports.setChatHistoryLines(50);
        }
    }

    public void saveConfig() {
        configLock.writeLock().lock();
        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            
            Yaml yaml = new Yaml(options);
            Map<String, Object> configMap = convertConfigToYaml();
            
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, yaml.dump(configMap));
            
            logger.info("Configuration saved successfully to {}", configPath);
        } catch (IOException e) {
            logger.error("Failed to save configuration: {}", e.getMessage(), e);
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertConfigToYaml() {
        Map<String, Object> result = new HashMap<>();
        
        // Auth Configuration
        Map<String, Object> auth = new HashMap<>();
        auth.put("token", config.getAuth().getToken());
        auth.put("regenerateToken", config.getAuth().isRegenerateToken());
        result.put("auth", auth);
        
        // Database Configuration
        Map<String, Object> database = new HashMap<>();
        database.put("type", config.getDatabase().getType());
        
        // MySQL Config
        Map<String, Object> mysql = new HashMap<>();
        mysql.put("host", config.getDatabase().getMysql().getHost());
        mysql.put("port", config.getDatabase().getMysql().getPort());
        mysql.put("database", config.getDatabase().getMysql().getDatabase());
        mysql.put("username", config.getDatabase().getMysql().getUsername());
        mysql.put("password", config.getDatabase().getMysql().getPassword());
        database.put("mysql", mysql);
        
        // File Storage Config
        Map<String, Object> file = new HashMap<>();
        file.put("path", config.getDatabase().getFile().getPath());
        database.put("file", file);
        
        result.put("database", database);
        
        // Reports Configuration
        Map<String, Object> reports = new HashMap<>();
        reports.put("cooldownSeconds", config.getReports().getCooldownSeconds());
        reports.put("allowAnonymousReports", config.getReports().isAllowAnonymousReports());
        reports.put("maxActiveReportsPerPlayer", config.getReports().getMaxActiveReportsPerPlayer());
        reports.put("chatHistoryLines", config.getReports().getChatHistoryLines());
        reports.put("logInventory", config.getReports().isLogInventory());
        reports.put("logLocation", config.getReports().isLogLocation());
        result.put("reports", reports);
        
        // Security Configuration
        Map<String, Object> security = new HashMap<>();
        security.put("maxFalseReports", config.getSecurity().getMaxFalseReports());
        security.put("falseReportBanHours", config.getSecurity().getFalseReportBanHours());
        security.put("maxReportsBeforeFlag", config.getSecurity().getMaxReportsBeforeFlag());
        security.put("encryptionKey", config.getSecurity().getEncryptionKey());
        security.put("enableTokenAuth", config.getSecurity().isEnableTokenAuth());
        result.put("security", security);
        
        // Notifications Configuration
        Map<String, Object> notifications = new HashMap<>();
        notifications.put("enableChatNotifications", config.getNotifications().isEnableChatNotifications());
        notifications.put("enableTitleNotifications", config.getNotifications().isEnableTitleNotifications());
        notifications.put("enableSoundNotifications", config.getNotifications().isEnableSoundNotifications());
        notifications.put("notificationSound", config.getNotifications().getNotificationSound());
        result.put("notifications", notifications);
        
        // Performance Configuration
        Map<String, Object> performance = new HashMap<>();
        performance.put("enableCaching", config.getPerformance().isEnableCaching());
        performance.put("cacheDuration", config.getPerformance().getCacheDuration());
        performance.put("maxCacheSize", config.getPerformance().getMaxCacheSize());
        performance.put("asyncProcessing", config.getPerformance().isAsyncProcessing());
        performance.put("batchSize", config.getPerformance().getBatchSize());
        performance.put("backgroundTaskInterval", config.getPerformance().getBackgroundTaskInterval());
        performance.put("cacheCleanupInterval", config.getPerformance().getCacheCleanupInterval());
        result.put("performance", performance);

        // Custom Reasons
        if (config.getCustomReasons() != null) {
            result.put("customReasons", new HashMap<>(config.getCustomReasons()));
        }

        return result;
    }

    public @Nullable String getMessage(String key) {
        // Return a proper message based on the key
        // This is a placeholder implementation - in a real scenario, this would retrieve from a messages file
        if (key == null) {
            return "";
        }
        
        // Default messages for common keys
        Map<String, String> defaultMessages = new HashMap<>();
        defaultMessages.put("messages.book.main-title", "Report Management");
        defaultMessages.put("messages.book.group-title", "Reports: {status}");
        defaultMessages.put("messages.book.view-details", "Click to view details");
        defaultMessages.put("messages.book.view-group", "View {count} {status} reports");
        defaultMessages.put("messages.errors.no-reports", "No {status} reports found.");
        defaultMessages.put("messages.errors.book-meta-null", "Error creating book.");
        defaultMessages.put("messages.errors.player-only", "This command is for players only.");
        defaultMessages.put("messages.errors.no-permission", "You don't have permission to use this command.");
        defaultMessages.put("messages.errors.invalid-group", "Invalid report status group.");
        
        return defaultMessages.getOrDefault(key, "");
    }

    public String getSymbol(String symbolKey) {
        // Return appropriate symbols based on the key
        Map<String, String> symbols = new HashMap<>();
        symbols.put("group_bullet", "• ");
        
        return symbols.getOrDefault(symbolKey, "");
    }

    public String getSeparator() {
        // Return a separator string for the book GUI
        return "------------------------";
    }

    public @NotNull String getFormattedMessage(String messageKey, String placeholder1, String value1, String placeholder2, String value2) {
        // Get the base message
        String message = getMessage(messageKey);
        if (message == null || message.isEmpty()) {
            return "Message not found: " + messageKey;
        }
        
        // Replace placeholders
        if (placeholder1 != null && value1 != null) {
            message = message.replace(placeholder1, value1);
        }
        
        if (placeholder2 != null && value2 != null) {
            message = message.replace(placeholder2, value2);
        }
        
        return message;
    }

    public int getReportsPerPage() {
        // Return the configured number of reports per page
        return 5; // Default value
    }

    public @NotNull Style getStatusColor(String statusName) {
        // Return appropriate color style based on report status
        if (statusName == null) {
            return Style.empty();
        }
        
        switch (statusName.toUpperCase()) {
            case "PENDING":
                return Style.style(net.kyori.adventure.text.format.NamedTextColor.GOLD);
            case "IN_PROGRESS":
                return Style.style(net.kyori.adventure.text.format.NamedTextColor.YELLOW);
            case "RESOLVED":
                return Style.style(net.kyori.adventure.text.format.NamedTextColor.GREEN);
            case "REJECTED":
                return Style.style(net.kyori.adventure.text.format.NamedTextColor.RED);
            default:
                return Style.style(net.kyori.adventure.text.format.NamedTextColor.GRAY);
        }
    }

    public String getText(String key) {
        // Return text content based on the key
        Map<String, String> textMap = new HashMap<>();
        textMap.put("messages.report-format.number-prefix", "#");
        textMap.put("gui.spacing.after_number", ": ");
        
        return textMap.getOrDefault(key, "");
    }

    @Data
    public static class Config {
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
            private FileStorageConfig file = new FileStorageConfig();

            @Data
            public static class MySQLConfig {
                private String host = "localhost";
                private int port = 3306;
                private String database = "aevorinreports";
                private String username = "root";
                private String password = "";
            }

            @Data
            public static class FileStorageConfig {
                private String path = "database/reports.db";
            }
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

}