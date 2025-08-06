package dev.aevorinstudios.aevorinReports.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerNameResolver {
    public static String resolvePlayerName(UUID uuid) {
        if (uuid == null) return "Unknown";
        
        // Try to get online player first
        var player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        
        // Fallback to offline player
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
    }
    
    public static CompletableFuture<String> resolvePlayerNameAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> resolvePlayerName(uuid));
    }
}