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

                JsonObject[] allVersions = JsonParser.parseString(response.toString()).getAsJsonArray().asList()
                        .stream()
                        .map(element -> element.getAsJsonObject())
                        .toArray(JsonObject[]::new);

                String channel = plugin.getConfigManager().getConfig().getUpdateChecker().getUpdateChannel()
                        .toLowerCase();

                java.util.Optional<JsonObject> latestVersionObj = java.util.Arrays.stream(allVersions)
                        .filter(v -> {
                            String type = v.get("version_type").getAsString().toLowerCase();
                            if (channel.equals("all"))
                                return true;
                            if (channel.equals("alpha"))
                                return type.equals("alpha");
                            if (channel.equals("beta"))
                                return type.equals("beta");
                            return type.equals("release");
                        })
                        .findFirst();

                if (latestVersionObj.isPresent()) {
                    JsonObject latest = latestVersionObj.get();
                    latestVersion = latest.get("version_number").getAsString();
                    downloadUrl = latest.getAsJsonArray("files").get(0).getAsJsonObject().get("url").getAsString();

                    String currentVersion = plugin.getDescription().getVersion();
                    updateAvailable = isNewer(latestVersion, currentVersion);

                    if (updateAvailable) {
                        plugin.getLogger().info(
                                "A new version of AevorinReports (" + channel + ") is available: " + latestVersion);
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

    private boolean isNewer(String latest, String current) {
        if (latest == null || current == null || latest.equalsIgnoreCase(current))
            return false;

        try {
            String[] latestSub = latest.split("-", 2);
            String[] currentSub = current.split("-", 2);

            String latestBase = latestSub[0];
            String currentBase = currentSub[0];

            int baseComparison = compareVersions(latestBase, currentBase);
            if (baseComparison != 0)
                return baseComparison > 0;

            // Base versions are equal (e.g. 1.0.6 vs 1.0.6-Beta)
            boolean latestHasSuffix = latestSub.length > 1;
            boolean currentHasSuffix = currentSub.length > 1;

            if (!latestHasSuffix && currentHasSuffix)
                return true; // 1.0.6 > 1.0.6-Beta
            if (latestHasSuffix && !currentHasSuffix)
                return false; // 1.0.6-Beta < 1.0.6

            if (latestHasSuffix && currentHasSuffix) {
                // Both have suffixes (1.0.6-Beta-2 vs 1.0.6-Beta-1)
                return latestSub[1].compareToIgnoreCase(currentSub[1]) > 0;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? tryParsePart(parts1[i]) : 0;
            int p2 = i < parts2.length ? tryParsePart(parts2[i]) : 0;
            if (p1 != p2)
                return p1 - p2;
        }
        return 0;
    }

    private int tryParsePart(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (updateAvailable && player.hasPermission("aevorinreports.update") &&
                plugin.getConfigManager().getConfig().getUpdateChecker().isNotifyOnJoin()) {
            dev.aevorinstudios.aevorinReports.utils.SchedulerUtils.runTaskLater(plugin, player, () -> {
                String prefix = "&8[&bAevorinReports&8]&r ";
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        prefix + "&eA new version is available: &b" + latestVersion));
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        prefix + "&eDownload it from: &b" + downloadUrl));
            }, 40L); // Delay notification by 2 seconds after join

        }
    }

    public void startUpdateChecker() {
        long interval = plugin.getConfigManager().getConfig().getUpdateChecker().getCheckInterval();

        // Schedule periodic checks (initial delay 0 to run immediately but async)
        dev.aevorinstudios.aevorinReports.utils.SchedulerUtils.runTaskTimerAsynchronously(plugin, this::checkUpdate,
                0L,
                TimeUnit.MINUTES.toSeconds(interval) * 20L);
    }
}