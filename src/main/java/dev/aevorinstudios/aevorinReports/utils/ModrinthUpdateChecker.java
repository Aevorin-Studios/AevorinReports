package dev.aevorinstudios.aevorinReports.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class ModrinthUpdateChecker implements Listener {
    private final BukkitPlugin plugin;
    private final String projectId;
    private String latestVersion;
    private String downloadUrl;
    private boolean updateAvailable = false;

    public ModrinthUpdateChecker(BukkitPlugin plugin, String projectId) {
        this.plugin = plugin;
        this.projectId = projectId;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void checkUpdate() {
        try {
            URL url = new URL("https://api.modrinth.com/v2/project/" + projectId + "/version");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "AevorinReports/" + plugin.getDescription().getVersion());

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonObject[] versions = JsonParser.parseString(response.toString()).getAsJsonArray().asList()
                    .stream()
                    .map(element -> element.getAsJsonObject())
                    .toArray(JsonObject[]::new);

                if (versions.length > 0) {
                    JsonObject latest = versions[0];
                    latestVersion = latest.get("version_number").getAsString();
                    downloadUrl = latest.getAsJsonArray("files").get(0).getAsJsonObject().get("url").getAsString();

                    String currentVersion = plugin.getDescription().getVersion();
                    updateAvailable = !currentVersion.equals(latestVersion);

                    if (updateAvailable) {
                        plugin.getLogger().info("A new version of AevorinReports is available: " + latestVersion);
                        plugin.getLogger().info("Download it from: " + downloadUrl);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (updateAvailable && player.hasPermission("aevorinreports.update") && 
            plugin.getConfig().getBoolean("update-checker.notify-on-join", true)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String prefix = plugin.getConfig().getString("notifications.prefix", "&8[&bAevorinReports&8]&r ");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    prefix + "&eA new version is available: &b" + latestVersion));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    prefix + "&eDownload it from: &b" + downloadUrl));
            }, 40L); // Delay notification by 2 seconds after join
        }
    }

    public void startUpdateChecker() {
        long interval = plugin.getConfig().getLong("update-checker.check-interval", 60);
        // Initial check
        checkUpdate();
        // Schedule periodic checks
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkUpdate, 
            TimeUnit.MINUTES.toSeconds(interval) * 20L, 
            TimeUnit.MINUTES.toSeconds(interval) * 20L);
    }
}