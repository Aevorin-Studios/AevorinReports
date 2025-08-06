package dev.aevorinstudios.aevorinReports.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.aevorinstudios.aevorinReports.config.VelocityConfigManager;
import dev.aevorinstudios.aevorinReports.database.DatabaseManager;
import dev.aevorinstudios.aevorinReports.sync.VelocityTokenManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Plugin(id = "aevorinreports", name = "AevorinReports", version = "1.0.3",
        description = "A comprehensive reporting system for Minecraft servers",
        authors = {"borhani", "Aiversity"})
public class VelocityPlugin {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private DatabaseManager databaseManager;
    private VelocityTokenManager tokenManager;
    private VelocityConfigManager configManager;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Initialize configuration
        try {
            configManager = new VelocityConfigManager(dataDirectory);
            configManager.loadConfig();
            logger.info("Configuration loaded successfully. Token: " + configManager.getToken());
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
            return;
        }

        // Initialize database connection
        initializeDatabase();

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        // Initialize token manager
        tokenManager = new VelocityTokenManager(this, server, configManager.getToken());
        server.getEventManager().register(this, tokenManager);

        logger.info("AevorinReports has been enabled!");
    }

    public Logger getLogger() {
        return logger;
    }

    public void onDisable() {
        if (tokenManager != null) {
            tokenManager.cleanup();
        }
    }

    // Configuration is now handled by VelocityConfigManager

    private void initializeDatabase() {
        // TODO: Load database credentials from config and initialize DatabaseManager
    }

    private void registerCommands() {
        // TODO: Register velocity commands
    }

    private void registerListeners() {
        // TODO: Register event listeners
    }

    // Token generation is now handled by VelocityConfigManager

    public ProxyServer getServer() {
        return server;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }
}