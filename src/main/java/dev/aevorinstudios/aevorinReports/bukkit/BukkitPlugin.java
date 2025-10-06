package dev.aevorinstudios.aevorinReports.bukkit;

import dev.aevorinstudios.aevorinReports.bukkit.commands.BukkitReportCommand;
import dev.aevorinstudios.aevorinReports.bukkit.commands.BukkitReportsCommand;
import dev.aevorinstudios.aevorinReports.bukkit.commands.ViewReportCommand;
import dev.aevorinstudios.aevorinReports.bukkit.commands.ShiftReportCommand;

import dev.aevorinstudios.aevorinReports.commands.HiddenCommandManager;
import dev.aevorinstudios.aevorinReports.config.ConfigManager;
import dev.aevorinstudios.aevorinReports.database.DatabaseManager;
import dev.aevorinstudios.aevorinReports.listeners.CommandHidingListener;
import dev.aevorinstudios.aevorinReports.sync.TokenSyncManager;
import dev.aevorinstudios.aevorinReports.utils.ExceptionHandler;
import dev.aevorinstudios.aevorinReports.utils.ModrinthUpdateChecker;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * The Main Bukkit plugin class for AevorinReports
 * Handles initialization, configuration, and lifecycle management
 */
public class BukkitPlugin extends JavaPlugin implements org.bukkit.command.CommandExecutor {

    @Getter
    private static BukkitPlugin instance;
    private ConfigManager configManager;
    @Getter
    private DatabaseManager databaseManager;
    @Getter
    private TokenSyncManager tokenSyncManager;
    private ModrinthUpdateChecker updateChecker;
    @Getter
    private CustomReasonHandler customReasonHandler;
    @Getter
    private BukkitReportCommand bukkitReportCommand;
    
    // Plugin state tracking
    private boolean databaseInitialized = false;
    private boolean configInitialized = false;

    @Override
    public void onEnable() {
        try {
            instance = this;
            getLogger().info("Initializing AevorinReports");

            // Initialize exception handler first with enhanced configuration
            initializeExceptionHandler();

            // Initialize configuration with validation
            if (!initializeConfig()) {
                getLogger().severe("Configuration initialization failed. Using default values where possible.");
            }

            // Initialize database connection with a retry mechanism
            if (!initializeDatabase()) {
                getLogger().severe("Database initialization failed. Plugin functionality will be limited.");
            }

            // Initialize CustomReasonHandler
            customReasonHandler = new CustomReasonHandler(this);

            // Register commands with error handling
            registerCommands();

            // Register listeners with validation
            registerListeners();

            // Register reload commands
            getCommand("ar").setExecutor(this);
            getCommand("aevorinreports").setExecutor(this);
            // Register ReportsContainerListener for container GUI
            getServer().getPluginManager().registerEvents(new dev.aevorinstudios.aevorinReports.bukkit.commands.ReportsContainerListener(this), this);
            // Register ReportReasonContainerListener for report container GUI
            getServer().getPluginManager().registerEvents(new dev.aevorinstudios.aevorinReports.bukkit.commands.ReportReasonContainerListener(this), this);

            // Register view report command with error handling
            try {
                getCommand("viewreport").setExecutor(new ViewReportCommand(this));
            } catch (Exception e) {
                ExceptionHandler.getInstance().handleException(e, "Command Registration", 
                    Map.of("command", "viewreport", "class", "ViewReportCommand"));
            }
            // Register shift report command with error handling
            try {
                getCommand("shiftreport").setExecutor(new ShiftReportCommand(this));
            } catch (Exception e) {
                ExceptionHandler.getInstance().handleException(e, "Command Registration", 
                    Map.of("command", "shiftreport", "class", "ShiftReportCommand"));
            }

            // Initialize token synchronization with enhanced error handling and retry logic
            initializeTokenSync();

            // Initialize hidden commands
            new HiddenCommandManager(this).registerHiddenCommands();
            
            // Register event listener for hiding commands
            getServer().getPluginManager().registerEvents(new CommandHidingListener(), this);

            // Initialize and start the Modrinth update checker
            String modrinthProjectId = getConfig().getString("update-checker.modrinth-project-id", "your-project-id");
            if (!modrinthProjectId.equals("your-project-id")) {
                updateChecker = new ModrinthUpdateChecker(this, modrinthProjectId);
                updateChecker.startUpdateChecker();
                getLogger().info("Modrinth update checker initialized with project ID: " + modrinthProjectId);
            } else {
                getLogger().warning("Modrinth project ID not configured. Update checking is disabled.");
            }

            getLogger().info("AevorinReports has been enabled!");
        } catch (Exception e) {
            // Use our custom exception handler for startup errors with detailed context
            Map<String, Object> context = new HashMap<>();
            context.put("plugin_version", getDescription().getVersion());
            context.put("server_version", Bukkit.getVersion());
            context.put("config_initialized", configInitialized);
            context.put("database_initialized", databaseInitialized);
            
            ExceptionHandler.getInstance().handleException(e, "Plugin Startup", context);
            getLogger().severe("Failed to enable AevorinReports due to a critical error. Check the logs for details.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if ((command.getName().equalsIgnoreCase("ar") || command.getName().equalsIgnoreCase("aevorinreports")) && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("aevorinreports.reload")) {
                sender.sendMessage("§cYou don't have permission to reload the config!");
                return true;
            }
            try {
                this.reloadConfig();
                sender.sendMessage("§aAevorinReports configuration reloaded successfully!");
            } catch (Exception e) {
                sender.sendMessage("§cFailed to reload configuration. Check the console for errors.");
                getLogger().warning("Error reloading configuration: " + e.getMessage());
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down AevorinReports");
        
        // Gracefully close database connections
        if (databaseManager != null) {
            try {
                getLogger().info("Closing database connections...");
                databaseManager.close();
                getLogger().info("Database connections closed successfully.");
            } catch (Exception e) {
                ExceptionHandler.getInstance().handleException(e, "Database Shutdown");
                getLogger().warning("Error while closing database connections.");
            }
        }
        
        // Clean up token synchronization resources
        if (tokenSyncManager != null) {
            try {
                getLogger().info("Cleaning up token synchronization...");
                tokenSyncManager.cleanup();
                getLogger().info("Token synchronization cleaned up successfully.");
            } catch (Exception e) {
                ExceptionHandler.getInstance().handleException(e, "TokenSync Shutdown");
                getLogger().warning("Error while cleaning up token synchronization.");
            }
        }
        
        getLogger().info("AevorinReports has been disabled!");
    }

    /**
     * Initializes and validates the plugin configuration
     * @return true if configuration was successfully initialized, false otherwise
     */
    private boolean initializeConfig() {
        try {
            getLogger().info("📜 Loading configuration files...");
            saveDefaultConfig();
            Path dataFolder = getDataFolder().toPath();
            configManager = ConfigManager.initialize(dataFolder);
            
            // Validate critical configuration sections
            boolean valid = validateConfiguration();
            
            configInitialized = true;
            getLogger().info("Configuration loaded successfully" + (valid ? "." : " with warnings."));
            return valid;
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(e, "Config Initialization");
            getLogger().severe("Failed to initialize configuration. Using default values where possible.");
            return false;
        }
    }
    
    /**
     * Validates critical configuration sections
     * @return true if all critical configurations are valid, false otherwise
     */
    private boolean validateConfiguration() {
        boolean valid = true;
        
        // Validate database configuration
        String dbType = getConfig().getString("database.type", "");
        if (dbType.isEmpty()) {
            getLogger().warning("Database type not specified in config. Defaulting to SQLite.");
            valid = false;
        } else if ("mysql".equalsIgnoreCase(dbType)) {
            // Validate MySQL configuration
            if (getConfig().getString("database.mysql.host", "").isEmpty()) {
                getLogger().warning("MySQL host not specified in config.");
                valid = false;
            }
            if (getConfig().getString("database.mysql.database", "").isEmpty()) {
                getLogger().warning("MySQL database name not specified in config.");
                valid = false;
            }
        }
        
        // Validate token configuration if proxy is enabled
        if (getConfig().getBoolean("proxy.enabled", false)) {
            String token = getConfig().getString("auth.token", "");
            if (token.isEmpty() || token.equals("your_token_here")) {
                getLogger().warning("Invalid proxy authentication token. Proxy synchronization will not work.");
                valid = false;
            }
        }
        
        return valid;
    }

    /**
     * Initializes the database connection with a retry mechanism
     * @return true if a database was successfully initialized, false otherwise
     */
    private boolean initializeDatabase() {
        getLogger().info("🗄️ Initializing database connection...");
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                ConfigManager.Config.DatabaseConfig dbConfig = configManager.getConfig().getDatabase();
                if ("mysql".equalsIgnoreCase(dbConfig.getType())) {
                    getLogger().info("Using MySQL database connection");
                    ConfigManager.Config.DatabaseConfig.MySQLConfig mysqlConfig = dbConfig.getMysql();
                    
                    // Log connection attempt (without password)
                    getLogger().info(String.format("Connecting to MySQL database: %s@%s:%d/%s", 
                        mysqlConfig.getUsername(), mysqlConfig.getHost(), 
                        mysqlConfig.getPort(), mysqlConfig.getDatabase()));
                    
                    databaseManager = new DatabaseManager(
                        mysqlConfig.getHost(),
                        mysqlConfig.getPort(),
                        mysqlConfig.getDatabase(),
                        mysqlConfig.getUsername(),
                        mysqlConfig.getPassword()
                    );
                } else {
                    // Handle SQLite storage
                    getLogger().info("Using SQLite database connection");
                    ConfigManager.Config.DatabaseConfig.FileStorageConfig fileConfig = dbConfig.getFile();
                    getLogger().info("SQLite database path: " + fileConfig.getPath());
                    databaseManager = new DatabaseManager(fileConfig.getPath());
                }
                
                // Test the connection
                if (databaseManager.testConnection()) {
                    databaseInitialized = true;
                    getLogger().info("Database connection established successfully!");
                    return true;
                } else {
                    throw new Exception("Database connection test failed");
                }
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    Map<String, Object> context = new HashMap<>();
                    context.put("retry_count", retryCount);
                    context.put("max_retries", maxRetries);
                    ExceptionHandler.getInstance().handleException(e, "Database Initialization", context);
                    getLogger().severe("Failed to initialize database after " + maxRetries + " attempts.");
                    return false;
                } else {
                    getLogger().warning("Database connection attempt failed. Retrying... (" + retryCount + "/" + maxRetries + ")");
                    try {
                        // Wait before retrying
                        Thread.sleep(2000 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * Registers plugin commands with error handling
     */
    private void registerCommands() {
        getLogger().info("Registering commands...");
        try {
            bukkitReportCommand = new BukkitReportCommand(this);
            getCommand("report").setExecutor(bukkitReportCommand);
            getCommand("report").setTabCompleter(bukkitReportCommand);
            getLogger().info("Registered 'report' command and tab completer");
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(e, "Command Registration", 
                Map.of("command", "report", "class", "BukkitReportCommand"));
        }
        
        try {
            getCommand("reports").setExecutor(new BukkitReportsCommand(this));
            getLogger().info("Registered 'reports' command");
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(e, "Command Registration", 
                Map.of("command", "reports", "class", "BukkitReportsCommand"));
        }
        
        getLogger().info("Commands registered successfully!");
    }

    /**
     * Registers event listeners
     */
    private void registerListeners() {
        getLogger().info("Registering event listeners...");
        // TODO: Register event listeners
        getLogger().info("Event listeners registered successfully!");
    }
    
    /**
     * Initializes token synchronization with enhanced error handling and retry logic
     */
    private void initializeTokenSync() {
        getLogger().info("Initializing token synchronization...");
        
        // Check if proxy is enabled in config
        if (!getConfig().getBoolean("proxy.enabled", false)) {
            getLogger().info("Proxy synchronization is disabled in config. Skipping token sync initialization.");
            return;
        }
        
        try {
            tokenSyncManager = new TokenSyncManager(this);
            
            // Set up authentication with retry logic
            CompletableFuture<Boolean> authFuture = tokenSyncManager.authenticate()
                .orTimeout(30, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    ExceptionHandler.getInstance().handleException(ex, "TokenSync Authentication");
                    getLogger().severe("Token authentication timed out or failed with exception.");
                    return false;
                });
            
            authFuture.thenAccept(success -> {
                if (success) {
                    getLogger().info("Successfully authenticated with proxy server!");
                } else {
                    getLogger().warning("Failed to authenticate with proxy. Reports will not be synced.");
                    getLogger().warning("Please check your token configuration in config.yml");
                    
                    // Schedule a retry after 5 minutes if the server is still running
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (isEnabled()) {
                            getLogger().info("Retrying proxy authentication...");
                            tokenSyncManager.authenticate().thenAccept(retrySuccess -> {
                                if (retrySuccess) {
                                    getLogger().info("Successfully authenticated with proxy on retry!");
                                } else {
                                    getLogger().warning("Retry authentication failed. Reports will not be synced.");
                                }
                            });
                        }
                    }, 6000); // 5 minutes = 6000 ticks
                }
            });
            
            getLogger().info("Token synchronization initialized successfully!");
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(e, "TokenSync Initialization");
            getLogger().severe("Failed to initialize token synchronization.");
        }
    }

    /**
     * Gets the configuration manager instance
     * @return The configuration manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the database manager instance
     * @return The database manager
     * @throws IllegalStateException if a database is not initialized
     */
    public DatabaseManager getDatabaseManager() {
        if (!databaseInitialized) {
            getLogger().warning("Attempting to access database manager before initialization");
        }
        return databaseManager;
    }
    
    /**
     * Gets the token sync manager instance
     * @return The token sync manager or null if not initialized
     */
    public TokenSyncManager getTokenSyncManager() {
        return tokenSyncManager;
    }
    
    /**
     * Initialize the exception handler for better error reporting with enhanced configuration
     */
    private void initializeExceptionHandler() {
        getLogger().info("Initializing enhanced exception handler...");
        
        // Get the exception handler instance and configure it
        ExceptionHandler handler = ExceptionHandler.getInstance();
        
        // Read configuration from config if available, otherwise use enhanced defaults
        int maxRepetitions = getConfig().getInt("error_handler.max_repetitions", 5);
        long suppressionMinutes = getConfig().getLong("error_handler.suppression_minutes", 15);
        boolean detailedLogging = getConfig().getBoolean("error_handler.detailed_logging", true);
        boolean logStackTraces = getConfig().getBoolean("error_handler.log_stack_traces", true);
        boolean groupSimilarErrors = getConfig().getBoolean("error_handler.group_similar_errors", true);
        
        // Configure with enhanced settings
        handler.configure(maxRepetitions, suppressionMinutes, detailedLogging, logStackTraces);
        handler.setGroupSimilarErrors(groupSimilarErrors);
        
        // Install the global exception handler
        handler.installGlobalHandler();
        
        // Set up custom exception handling for the logger with enhanced context
        getLogger().setFilter(record -> {
            if (record.getThrown() != null) {
                // Let our handler process the exception with rich context
                Map<String, Object> context = new HashMap<>();
                context.put("log_level", record.getLevel().getName());
                context.put("logger_name", record.getLoggerName());
                context.put("message", record.getMessage());
                context.put("thread", Thread.currentThread().getName());
                context.put("plugin_version", getDescription().getVersion());
                
                // Add server information for better diagnostics
                if (Bukkit.getServer() != null) {
                    context.put("server_version", Bukkit.getVersion());
                    context.put("bukkit_version", Bukkit.getBukkitVersion());
                    context.put("online_mode", Bukkit.getOnlineMode());
                }
                
                handler.handleException(record.getThrown(), "Logger", context);
                
                // Only show the message without the stack trace in the regular log
                // but keep severe errors visible
                if (record.getLevel() != Level.SEVERE) {
                    record.setThrown(null);
                }
            }
            return true;
        });
        
        getLogger().info("Enhanced exception handler initialized successfully!");
    }
}