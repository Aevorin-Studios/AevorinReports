package dev.aevorinstudios.aevorinReports.bukkit;

import dev.aevorinstudios.aevorinReports.commands.BukkitReportCommand;
import dev.aevorinstudios.aevorinReports.commands.BukkitReportsCommand;
import dev.aevorinstudios.aevorinReports.commands.ViewReportCommand;
import dev.aevorinstudios.aevorinReports.commands.SetReportStatusCommand;

import dev.aevorinstudios.aevorinReports.config.ConfigManager;
import dev.aevorinstudios.aevorinReports.database.DatabaseManager;
import dev.aevorinstudios.aevorinReports.handlers.CustomReasonHandler;
import dev.aevorinstudios.aevorinReports.utils.ExceptionHandler;
import dev.aevorinstudios.aevorinReports.utils.ModrinthUpdateChecker;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


/**
 * The Main Bukkit plugin class for AevorinReports
 * Handles initialization, configuration, and lifecycle management
 */
public class BukkitPlugin extends JavaPlugin implements org.bukkit.command.CommandExecutor {

    @Getter
    private static BukkitPlugin instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
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


            // Initialize and start the Modrinth update checker
            String modrinthProjectId = "OwqSnlXx"; // Hardcoded Project ID
            updateChecker = new ModrinthUpdateChecker(this, modrinthProjectId);
            updateChecker.startUpdateChecker();
            getLogger().info("Modrinth update checker initialized with project ID: " + modrinthProjectId);

            // Initialize bStats Metrics
            int pluginId = 28310;
            new org.bstats.bukkit.Metrics(this, pluginId);
            getLogger().info("bStats Metrics initialized properly.");

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
        
        getLogger().info("AevorinReports has been disabled!");
    }

    /**
     * Initializes and validates the plugin configuration
     * @return true if configuration was successfully initialized, false otherwise
     */
    private boolean initializeConfig() {
        try {
            getLogger().info("Loading configuration files...");
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
        String dbType = configManager.getConfig().getDatabase().getType();
        if (dbType == null || dbType.isEmpty()) {
            configManager.getConfig().getDatabase().setType("file"); // Default to file
            getLogger().warning("Database type not specified in config. Defaulting to SQLite.");
            // We validly fell back, so don't return false unless critical
        } else if ("mysql".equalsIgnoreCase(dbType)) {
            // Validate MySQL configuration
            if (configManager.getConfig().getDatabase().getMysql().getHost().isEmpty()) {
                getLogger().warning("MySQL host not specified in config.");
                valid = false;
            }
            if (configManager.getConfig().getDatabase().getMysql().getDatabase().isEmpty()) {
                getLogger().warning("MySQL database name not specified in config.");
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
        getLogger().info("Initializing database connection...");
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
                    
                    // Initialize or retrieve persistent server token
                    dev.aevorinstudios.aevorinReports.utils.ServerIdentity identity = 
                        new dev.aevorinstudios.aevorinReports.utils.ServerIdentity(getLogger(), getDataFolder());
                    String serverToken = identity.getIdentityToken();
                    
                    // Sync this server's identity (Name <-> Token) with the database
                    String serverName = configManager.getConfig().getServerName();
                    databaseManager.syncServerIdentity(serverToken, serverName);
                    
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
        
        try {
            getCommand("viewreport").setExecutor(new ViewReportCommand(this));
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(e, "Command Registration", 
                Map.of("command", "viewreport", "class", "ViewReportCommand"));
        }
        
        try {
            SetReportStatusCommand setReportStatusCommand = new SetReportStatusCommand(this);
            getCommand("setreportstatus").setExecutor(setReportStatusCommand);
            getCommand("setreportstatus").setTabCompleter(setReportStatusCommand);
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(e, "Command Registration", 
                Map.of("command", "setreportstatus", "class", "SetReportStatusCommand"));
        }

        getLogger().info("Commands registered successfully!");
    }

    /**
     * Registers event listeners
     */
    private void registerListeners() {
        getLogger().info("Registering event listeners...");
        
        // Register ReportsContainerListener for container GUI
        getServer().getPluginManager().registerEvents(new dev.aevorinstudios.aevorinReports.listeners.ReportsContainerListener(this), this);
        
        // Register ReportReasonContainerListener for report container GUI
        getServer().getPluginManager().registerEvents(new dev.aevorinstudios.aevorinReports.listeners.ReportReasonContainerListener(this), this);
        
        getLogger().info("Event listeners registered successfully!");
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
    }
}