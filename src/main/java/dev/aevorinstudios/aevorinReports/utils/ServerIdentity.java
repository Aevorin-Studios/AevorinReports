package dev.aevorinstudios.aevorinReports.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages the unique server identity token stored in the server's root directory.
 * This ensures the server keeps its identity even if plugin configurations are deleted.
 */
public class ServerIdentity {
    private static final String TOKEN_FILENAME = "aevorin_server_token.dat";
    private final Logger logger;
    private final File dataFolder;

    public ServerIdentity(Logger logger, File dataFolder) {
        this.logger = logger;
        this.dataFolder = dataFolder;
    }

    /**
     * Retrieves the persistent server token.
     * If it doesn't exist, a new one is generated and saved.
     * 
     * @return The unique server token
     */
    public String getIdentityToken() {
        // We look in the server root directory (parent of plugins folder)
        // dataFolder is usually plugins/AevorinReports
        // dataFolder.getParentFile() is plugins/
        // dataFolder.getParentFile().getParentFile() is the server root
        
        File rootDir = dataFolder.getParentFile().getParentFile();
        File tokenFile = new File(rootDir, TOKEN_FILENAME);

        try {
            if (tokenFile.exists()) {
                String token = Files.readString(tokenFile.toPath(), StandardCharsets.UTF_8).trim();
                if (!token.isEmpty()) {
                    return token;
                }
            }

            // Generate new token
            String newToken = UUID.randomUUID().toString().replace("-", "") + 
                              UUID.randomUUID().toString().replace("-", ""); // 64 chars
            
            Files.writeString(tokenFile.toPath(), newToken, StandardCharsets.UTF_8);
            logger.info("Generated new server identity token: " + TOKEN_FILENAME);
            
            return newToken;

        } catch (IOException e) {
            logger.severe("Failed to handle server identity token file: " + e.getMessage());
            e.printStackTrace();
            // Fallback: don't save, just return a temp one (bad, but prevents crash)
            return UUID.randomUUID().toString();
        }
    }
}
