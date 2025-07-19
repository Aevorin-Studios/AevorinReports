package dev.aevorinstudios.aevorinReports.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportConfiguration {
    private static ReportConfiguration instance;
    private List<String> categories;
    private int cooldown;
    private int maxActiveReports;
    private int minDescriptionLength;
    private int maxDescriptionLength;

    private ReportConfiguration() {
        categories = new ArrayList<>();
        loadDefaults();
    }

    public static ReportConfiguration getInstance() {
        if (instance == null) {
            instance = new ReportConfiguration();
        }
        return instance;
    }

    private void loadDefaults() {
        categories.add("Hacking");
        categories.add("Griefing");
        categories.add("Chat Abuse");
        categories.add("Other");
        cooldown = 300;
        maxActiveReports = 3;
        minDescriptionLength = 10;
        maxDescriptionLength = 500;
    }

    @SuppressWarnings("unchecked")
    public void loadConfig(InputStream configFile) {
        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.load(configFile);

        if (config != null && config.containsKey("reports")) {
            Map<String, Object> reportsConfig = (Map<String, Object>) config.get("reports");
            
            if (reportsConfig.containsKey("categories")) {
                categories = (List<String>) reportsConfig.get("categories");
            }
            
            if (reportsConfig.containsKey("cooldown")) {
                cooldown = (int) reportsConfig.get("cooldown");
            }
            
            if (reportsConfig.containsKey("max-active-reports")) {
                maxActiveReports = (int) reportsConfig.get("max-active-reports");
            }
            
            if (reportsConfig.containsKey("min-description-length")) {
                minDescriptionLength = (int) reportsConfig.get("min-description-length");
            }
            
            if (reportsConfig.containsKey("max-description-length")) {
                maxDescriptionLength = (int) reportsConfig.get("max-description-length");
            }
        }
    }

    public List<String> getCategories() {
        return new ArrayList<>(categories);
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getMaxActiveReports() {
        return maxActiveReports;
    }

    public int getMinDescriptionLength() {
        return minDescriptionLength;
    }

    public int getMaxDescriptionLength() {
        return maxDescriptionLength;
    }
}