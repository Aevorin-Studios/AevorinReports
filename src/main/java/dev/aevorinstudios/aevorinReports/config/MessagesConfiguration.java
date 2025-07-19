package dev.aevorinstudios.aevorinReports.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class MessagesConfiguration {
    private static MessagesConfiguration instance;
    
    // Error messages
    private String noPermission;
    private String playerOnly;
    private String invalidGroup;
    private String noReports;
    
    // Book messages
    private String mainTitle;
    private String groupTitle;
    private String viewDetails;
    private String viewGroup;
    
    // Report format
    private String numberPrefix;
    private String statusFormat;
    private String separator;
    
    private MessagesConfiguration() {
        loadDefaults();
    }
    
    public static MessagesConfiguration getInstance() {
        if (instance == null) {
            instance = new MessagesConfiguration();
        }
        return instance;
    }
    
    private void loadDefaults() {
        // Error messages
        noPermission = "&cYou don't have permission to use this command!";
        playerOnly = "&cOnly players can use this command!";
        invalidGroup = "&cInvalid group! Available groups: PENDING, IN_PROGRESS, RESOLVED, REJECTED";
        noReports = "&cNo {status} reports found.";
        
        // Book messages
        mainTitle = "&6Report Groups";
        groupTitle = "&6{status} Reports";
        viewDetails = "Click to view report details";
        viewGroup = "Click to view {count} {status} reports";
        
        // Report format
        numberPrefix = "#";
        statusFormat = "[{status}]";
        separator = "⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯";
    }
    
    @SuppressWarnings("unchecked")
    public void loadConfig(InputStream configFile) {
        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.load(configFile);
        
        if (config != null && config.containsKey("messages")) {
            Map<String, Object> messages = (Map<String, Object>) config.get("messages");
            
            if (messages.containsKey("errors")) {
                Map<String, Object> errors = (Map<String, Object>) messages.get("errors");
                noPermission = (String) errors.getOrDefault("no-permission", noPermission);
                playerOnly = (String) errors.getOrDefault("player-only", playerOnly);
                invalidGroup = (String) errors.getOrDefault("invalid-group", invalidGroup);
                noReports = (String) errors.getOrDefault("no-reports", noReports);
            }
            
            if (messages.containsKey("book")) {
                Map<String, Object> book = (Map<String, Object>) messages.get("book");
                mainTitle = (String) book.getOrDefault("main-title", mainTitle);
                groupTitle = (String) book.getOrDefault("group-title", groupTitle);
                viewDetails = (String) book.getOrDefault("view-details", viewDetails);
                viewGroup = (String) book.getOrDefault("view-group", viewGroup);
            }
            
            if (messages.containsKey("report-format")) {
                Map<String, Object> format = (Map<String, Object>) messages.get("report-format");
                numberPrefix = (String) format.getOrDefault("number-prefix", numberPrefix);
                statusFormat = (String) format.getOrDefault("status-format", statusFormat);
                separator = (String) format.getOrDefault("separator", separator);
            }
        }
    }
    
    // Getters
    public String getNoPermission() { return noPermission; }
    public String getPlayerOnly() { return playerOnly; }
    public String getInvalidGroup() { return invalidGroup; }
    public String getNoReports() { return noReports; }
    
    public String getMainTitle() { return mainTitle; }
    public String getGroupTitle() { return groupTitle; }
    public String getViewDetails() { return viewDetails; }
    public String getViewGroup() { return viewGroup; }
    
    public String getNumberPrefix() { return numberPrefix; }
    public String getStatusFormat() { return statusFormat; }
    public String getSeparator() { return separator; }
    
    // Utility methods for placeholder replacement
    public String formatNoReports(String status) {
        return noReports.replace("{status}", status);
    }
    
    public String formatGroupTitle(String status) {
        return groupTitle.replace("{status}", status);
    }
    
    public String formatViewGroup(int count, String status) {
        return viewGroup
            .replace("{count}", String.valueOf(count))
            .replace("{status}", status);
    }
    
    public String formatStatusFormat(String status) {
        return statusFormat.replace("{status}", status);
    }

    public String getMessage(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        String[] parts = path.split("\\.");
        if (parts.length < 2) {
            return "";
        }

        switch (parts[1]) {
            case "errors":
                switch (parts[2]) {
                    case "no-permission": return noPermission;
                    case "player-only": return playerOnly;
                    case "invalid-group": return invalidGroup;
                    case "no-reports": return noReports;
                    default: return "";
                }
            case "book":
                switch (parts[2]) {
                    case "main-title": return mainTitle;
                    case "group-title": return groupTitle;
                    case "view-details": return viewDetails;
                    case "view-group": return viewGroup;
                    default: return "";
                }
            case "report-format":
                switch (parts[2]) {
                    case "number-prefix": return numberPrefix;
                    case "status-format": return statusFormat;
                    case "separator": return separator;
                    default: return "";
                }
            default:
                return "";
        }
    }
}