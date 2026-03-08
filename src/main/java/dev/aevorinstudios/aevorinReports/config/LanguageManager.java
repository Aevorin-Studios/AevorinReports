package dev.aevorinstudios.aevorinReports.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class LanguageManager {
    private final Plugin plugin;
    private final Logger logger;
    private FileConfiguration langConfig;
    private File langFile;
    private final String langName;
    private static final Map<String, LanguageManager> instances = new HashMap<>();

    // List of officially supported languages bundled within the plugin jar
    private static final java.util.List<String> SUPPORTED_LANGUAGES = java.util.Arrays.asList("en_US");

    private LanguageManager(Plugin plugin, String langName) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.langName = langName;
        load();
    }

    public static LanguageManager get(Plugin plugin) {
        String lang = plugin.getConfig().getString("language", "en_US");
        return instances.computeIfAbsent(lang, k -> new LanguageManager(plugin, k));
    }

    public static void reloadAll(Plugin plugin) {
        instances.clear();
        get(plugin);
    }

    public void load() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        langFile = new File(langDir, langName + ".yml");
        boolean isSupported = SUPPORTED_LANGUAGES.contains(langName);

        if (!langFile.exists() && isSupported) {
            plugin.saveResource("lang/" + langName + ".yml", false);
        } else if (!langFile.exists()) {
            logger.warning(
                    "Language file " + langName + ".yml not found and is not a default language. Using missing keys.");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);

        if (isSupported) {
            // Load default values from JAR for supported languages
            InputStream defLangStream = plugin.getResource("lang/" + langName + ".yml");
            if (defLangStream != null) {
                YamlConfiguration defConfig = YamlConfiguration
                        .loadConfiguration(new InputStreamReader(defLangStream, StandardCharsets.UTF_8));

                int currentVersion = langConfig.getInt("config-version", 0);
                int latestVersion = defConfig.getInt("config-version", 0);

                if (latestVersion > currentVersion && currentVersion > 0) { // If it's 0 it might not exist yet
                    logger.info("Updating language file " + langName + ".yml (v" + currentVersion + " -> v"
                            + latestVersion + ")");

                    // Replace with the new file from JAR
                    plugin.saveResource("lang/" + langName + ".yml", true);
                    langConfig = YamlConfiguration.loadConfiguration(langFile); // Load the new config
                } else if (currentVersion == 0 && langFile.exists()) {
                    // For files that didn't have version tracking before
                    logger.info("Found outdated language file without version. Resetting to defaults...");
                    plugin.saveResource("lang/" + langName + ".yml", true);
                    langConfig = YamlConfiguration.loadConfiguration(langFile);
                }

                langConfig.setDefaults(defConfig);
            }
        } else {
            // It's a custom language file, load en_US as a fallback for missing keys, but DO NOT modify the file!
            InputStream fallbackStream = plugin.getResource("lang/en_US.yml");
            if (fallbackStream != null) {
                YamlConfiguration fallbackConfig = YamlConfiguration
                        .loadConfiguration(new InputStreamReader(fallbackStream, StandardCharsets.UTF_8));
                
                // Detect missing keys
                java.util.List<String> missingKeys = new java.util.ArrayList<>();
                for (String key : fallbackConfig.getKeys(true)) {
                    if (!fallbackConfig.isConfigurationSection(key) && !langConfig.contains(key)) {
                        missingKeys.add(key);
                    }
                }

                if (!missingKeys.isEmpty()) {
                    logger.warning("Custom language file (" + langName + ".yml) is missing " + missingKeys.size() + " translation keys!");
                    logger.warning("Falling back to en_US.yml defaults for those missing values to prevent errors.");
                }

                langConfig.setDefaults(fallbackConfig);
            }
        }
    }

    public String getRawMessage(String path) {
        return langConfig.getString(path, "Missing lang: " + path);
    }

    public String getMessage(String path) {
        return ChatColor.translateAlternateColorCodes('&', getRawMessage(path));
    }

    public String getMessage(String path, String defaultValue) {
        String message = langConfig.getString(path, defaultValue);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getRawMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public java.util.List<String> getMessageList(String path) {
        java.util.List<String> list = langConfig.getStringList(path);
        if (list.isEmpty()) {
            return java.util.Collections.singletonList("Missing lang list: " + path);
        }
        java.util.List<String> coloredList = new java.util.ArrayList<>();
        for (String s : list) {
            coloredList.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        return coloredList;
    }

    public java.util.List<String> getMessageList(String path, Map<String, String> placeholders) {
        java.util.List<String> list = langConfig.getStringList(path);
        java.util.List<String> replacedList = new java.util.ArrayList<>();
        for (String s : list) {
            String replaced = s;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                replaced = replaced.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            replacedList.add(ChatColor.translateAlternateColorCodes('&', replaced));
        }
        return replacedList;
    }

    public String getLocalizedStatus(dev.aevorinstudios.aevorinReports.reports.Report.ReportStatus status) {
        return getMessage("common.status." + status.name().toLowerCase(), status.name());
    }

    public String getLocalizedReason(String reason) {
        return reason;
    }

    public java.util.List<String> getReasonList() {
        return getMessageList("common.reasons");
    }
}
