package dev.aevorinstudios.aevorinReports.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class GUIConfiguration {
    private static GUIConfiguration instance;
    
    // Colors
    private String titleColor;
    private String numberColor;
    private String playerNameColor;
    private String separatorColor;
    
    // Status Colors
    private String pendingColor;
    private String inProgressColor;
    private String resolvedColor;
    private String rejectedColor;
    
    // Spacing
    private String afterNumber;
    private String afterName;
    private String beforeLine;
    private String afterLine;
    
    // Symbols
    private String groupBullet;
    private String groupCountOpen;
    private String groupCountClose;
    
    // Layout
    private String separatorLine;
    private int reportsPerPage;
    
    private GUIConfiguration() {
        loadDefaults();
    }
    
    public static GUIConfiguration getInstance() {
        if (instance == null) {
            instance = new GUIConfiguration();
        }
        return instance;
    }
    
    private void loadDefaults() {
        titleColor = "&6";
        numberColor = "&6";
        playerNameColor = "&f";
        separatorColor = "&8";
        
        pendingColor = "&d";
        inProgressColor = "&b";
        resolvedColor = "&a";
        rejectedColor = "&c";
        
        afterNumber = " ";
        afterName = " ";
        beforeLine = "";
        afterLine = "";
        
        groupBullet = "➤ ";
        groupCountOpen = "(";
        groupCountClose = ")";
        
        separatorLine = "⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯";
        reportsPerPage = 5;
    }
    
    @SuppressWarnings("unchecked")
    public void loadConfig(InputStream configFile) {
        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.load(configFile);
        
        if (config != null && config.containsKey("gui")) {
            Map<String, Object> guiConfig = (Map<String, Object>) config.get("gui");
            
            if (guiConfig.containsKey("colors")) {
                Map<String, Object> colors = (Map<String, Object>) guiConfig.get("colors");
                titleColor = (String) colors.getOrDefault("title", titleColor);
                numberColor = (String) colors.getOrDefault("number", numberColor);
                playerNameColor = (String) colors.getOrDefault("player_name", playerNameColor);
                separatorColor = (String) colors.getOrDefault("separator", separatorColor);
                
                if (colors.containsKey("status")) {
                    Map<String, Object> statusColors = (Map<String, Object>) colors.get("status");
                    pendingColor = (String) statusColors.getOrDefault("pending", pendingColor);
                    inProgressColor = (String) statusColors.getOrDefault("in_progress", inProgressColor);
                    resolvedColor = (String) statusColors.getOrDefault("resolved", resolvedColor);
                    rejectedColor = (String) statusColors.getOrDefault("rejected", rejectedColor);
                }
            }
            
            if (guiConfig.containsKey("spacing")) {
                Map<String, Object> spacing = (Map<String, Object>) guiConfig.get("spacing");
                afterNumber = (String) spacing.getOrDefault("after_number", afterNumber);
                afterName = (String) spacing.getOrDefault("after_name", afterName);
                beforeLine = (String) spacing.getOrDefault("before_line", beforeLine);
                afterLine = (String) spacing.getOrDefault("after_line", afterLine);
            }
            
            if (guiConfig.containsKey("symbols")) {
                Map<String, Object> symbols = (Map<String, Object>) guiConfig.get("symbols");
                groupBullet = (String) symbols.getOrDefault("group_bullet", groupBullet);
                groupCountOpen = (String) symbols.getOrDefault("group_count_open", groupCountOpen);
                groupCountClose = (String) symbols.getOrDefault("group_count_close", groupCountClose);
            }
            
            if (guiConfig.containsKey("layout")) {
                Map<String, Object> layout = (Map<String, Object>) guiConfig.get("layout");
                separatorLine = (String) layout.getOrDefault("separator_line", separatorLine);
                reportsPerPage = (int) layout.getOrDefault("reports_per_page", reportsPerPage);
            }
        }
    }
    
    // Getters
    public String getTitleColor() { return titleColor; }
    public String getNumberColor() { return numberColor; }
    public String getPlayerNameColor() { return playerNameColor; }
    public String getSeparatorColor() { return separatorColor; }
    
    public String getPendingColor() { return pendingColor; }
    public String getInProgressColor() { return inProgressColor; }
    public String getResolvedColor() { return resolvedColor; }
    public String getRejectedColor() { return rejectedColor; }
    
    public String getAfterNumber() { return afterNumber; }
    public String getAfterName() { return afterName; }
    public String getBeforeLine() { return beforeLine; }
    public String getAfterLine() { return afterLine; }
    
    public String getGroupBullet() { return groupBullet; }
    public String getGroupCountOpen() { return groupCountOpen; }
    public String getGroupCountClose() { return groupCountClose; }
    
    public String getSeparatorLine() { return separatorLine; }
    public int getReportsPerPage() { return reportsPerPage; }

    public String getSymbol(String symbolName) {
        return switch (symbolName) {
            case "group_bullet" -> groupBullet;
            case "group_count_open" -> groupCountOpen;
            case "group_count_close" -> groupCountClose;
            default -> throw new IllegalArgumentException("Unknown symbol: " + symbolName);
        };
    }
}