package dev.aevorinstudios.aevorinReports.config;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class VelocityConfigManager {
    private final Path configPath;
    private Toml config;

    public VelocityConfigManager(Path dataDirectory) {
        this.configPath = dataDirectory.resolve("velocity-config.toml");
    }

    public void loadConfig() throws IOException {
        if (!Files.exists(configPath)) {
            Files.createDirectories(configPath.getParent());
            copyDefaultConfig();
        }

        config = new Toml().read(configPath.toFile());
        handleTokenGeneration();
    }

    private void copyDefaultConfig() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("velocity-config.toml")) {
            if (is == null) {
                throw new IOException("Default config not found in resources");
            }
            Files.copy(is, configPath);
        }
    }

    private void handleTokenGeneration() throws IOException {
        Map<String, Object> authConfig = config.getTable("auth").to(Map.class);
        String token = (String) authConfig.get("token");
        boolean regenerateToken = (boolean) authConfig.getOrDefault("regenerate-token", false);

        if (token == null || token.isEmpty() || regenerateToken) {
            // Generate a new token
            token = generateSecureToken();
            
            // Instead of modifying the entire config structure, just update the token in the file
            String content = new String(Files.readAllBytes(configPath));
            String updatedContent = content.replaceFirst("token = \".*\"", "token = \"" + token + "\"");
            
            // Write the updated content back to the file
            Files.write(configPath, updatedContent.getBytes());
            
            // Reload the config
            config = new Toml().read(configPath.toFile());
        }
    }

    private String generateSecureToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public String getToken() {
        return config.getString("auth.token");
    }

    public Toml getConfig() {
        return config;
    }

    public boolean isCacheEnabled() {
        return config.getBoolean("performance.cache-enabled");
    }

    public int getMaxCachedReports() {
        return config.getLong("performance.max-cached-reports").intValue();
    }

    public int getCacheExpiration() {
        return config.getLong("performance.cache-expiration").intValue();
    }

    public boolean isAsyncProcessingEnabled() {
        return config.getBoolean("performance.async-processing");
    }

    public int getAsyncThreadPoolSize() {
        return config.getLong("performance.async-thread-pool-size").intValue();
    }

    public int getMaxAsyncQueueSize() {
        return config.getLong("performance.max-async-queue-size").intValue();
    }
}