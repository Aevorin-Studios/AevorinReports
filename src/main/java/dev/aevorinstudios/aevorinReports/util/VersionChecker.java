package dev.aevorinstudios.aevorinReports.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class VersionChecker {
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2";
    private static final String PROJECT_ID = "OwqSnlXx"; // Your Modrinth project ID or slug
    private static final String USER_AGENT = "AevorinReports/VersionChecker/1.0.3";
    private final BukkitPlugin plugin;
    private String latestVersion = null;
    
    public VersionChecker(BukkitPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Starts the version checker task
     */
    public void startVersionChecking() {
        // Check version every 6 hours (20 ticks * 60 seconds * 60 minutes * 6 hours)
        long checkInterval = 20L * 60L * 60L * 6L;
        
        // Initial delay of 5 minutes is to not slow down server startup
        long initialDelay = 20L * 60L * 5L;
        
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            checkVersion().thenAccept(hasUpdate -> {
                if (hasUpdate) {
                    notifyOperators();
                }
            });
        }, initialDelay, checkInterval);
    }
    
    /**
     * Checks for a new version of the plugin
     * @return CompletableFuture<Boolean> true if an update is available
     */
    private CompletableFuture<Boolean> checkVersion() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(MODRINTH_API_URL + "/project/" + PROJECT_ID + "/version");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", USER_AGENT);
                
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        
                        JsonArray versions = JsonParser.parseString(response.toString()).getAsJsonArray();
                        if (versions.size() > 0) {
                            JsonObject latestVersion = versions.get(0).getAsJsonObject();
                            String newVersion = latestVersion.get("version_number").getAsString();
                            String currentVersion = plugin.getDescription().getVersion();
                            
                            this.latestVersion = newVersion;
                            return compareVersions(currentVersion, newVersion) < 0;
                        }
                    }
                } else {
                    plugin.getLogger().warning("Failed to check for updates. Response code: " 
                            + connection.getResponseCode());
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error checking for updates", e);
            }
            return false;
        });
    }
    
    /**
     * Compares two version strings
     * @param current Current version
     * @param latest Latest version
     * @return negative if current < latest, 0 if equal, positive if current > latest
     */
    private int compareVersions(String current, String latest) {
        String[] currentParts = current.split("\\.");
        String[] latestParts = latest.split("\\.");
        
        int length = Math.max(currentParts.length, latestParts.length);
        
        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? 
                    Integer.parseInt(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? 
                    Integer.parseInt(latestParts[i]) : 0;
            
            if (currentPart < latestPart) return -1;
            if (currentPart > latestPart) return 1;
        }
        
        return 0;
    }
    
    /**
     * Notifies online operators about available updates
     */
    private void notifyOperators() {
        String message = ChatColor.GREEN + "[AevorinReports] " + 
                ChatColor.YELLOW + "A new version is available! " + 
                ChatColor.WHITE + "Current version: " + 
                ChatColor.RED + plugin.getDescription().getVersion() + 
                ChatColor.WHITE + ", Latest version: " + 
                ChatColor.GREEN + latestVersion + 
                ChatColor.WHITE + ". Please update from Modrinth.";
        
        // Schedule notification on The main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Notify console
            plugin.getLogger().info("A new version is available! Current version: " + 
                    plugin.getDescription().getVersion() + ", Latest version: " + latestVersion);
            
            // Notify online operators
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOp() || player.hasPermission("aevorinreports.update.notify")) {
                    player.sendMessage(message);
                }
            }
        });
    }
}