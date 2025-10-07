package dev.aevorinstudios.aevorinReports.handlers;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import net.md_5.bungee.api.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomReasonHandler implements Listener {
    private final BukkitPlugin plugin;
    private final Map<UUID, CustomReasonData> pendingCustomReasons;

    public CustomReasonHandler(BukkitPlugin plugin) {
        this.plugin = plugin;
        this.pendingCustomReasons = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void startCustomReason(Player player, String targetPlayer) {
        pendingCustomReasons.put(player.getUniqueId(), new CustomReasonData(targetPlayer));
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        CustomReasonData data = pendingCustomReasons.get(player.getUniqueId());
        
        if (data != null) {
            event.setCancelled(true); // Cancel the chat message
            String reason = event.getMessage();
            
            // Validate reason length
            int minLength = plugin.getConfig().getInt("reports.custom-reason-min-length", 10);
            int maxLength = plugin.getConfig().getInt("reports.custom-reason-max-length", 100);
            
            if (reason.length() < minLength) {
                player.sendMessage(ChatColor.RED + plugin.getConfig().getString("messages.custom-reason-too-short", 
                    "Your reason is too short. Minimum length is " + minLength + " characters."));
                return;
            }
            
            if (reason.length() > maxLength) {
                player.sendMessage(ChatColor.RED + plugin.getConfig().getString("messages.custom-reason-too-long", 
                    "Your reason is too long. Maximum length is " + maxLength + " characters."));
                return;
            }
            
            // Submit the report
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getBukkitReportCommand().createReport(player, data.targetPlayer(), reason);
                pendingCustomReasons.remove(player.getUniqueId());
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingCustomReasons.remove(event.getPlayer().getUniqueId());
    }

    private record CustomReasonData(String targetPlayer) {}
}