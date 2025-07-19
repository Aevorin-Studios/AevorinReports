package dev.aevorinstudios.aevorinReports.config;

import lombok.Data;

/**
 * Configuration for the error handling system.
 * Controls how errors are logged, formatted, and rate-limited.
 */
@Data
public class ErrorHandlerConfig {
    // Rate limiting settings
    private int maxErrorRepetitions = 3;
    private long suppressionTimeMinutes = 10;
    
    // Logging detail settings
    private boolean detailedLogging = true;
    private boolean logStackTraces = true;
    private boolean logToSeparateFile = false;
    private String logFilePath = "logs/error.log";
    
    // Error categorization
    private boolean categorizeErrors = true;
    private boolean groupSimilarErrors = true;
    
    // Console output settings
    private boolean useColoredOutput = true;
    private boolean showErrorCounts = true;
    
    /**
     * Creates a default configuration with balanced settings
     */
    public static ErrorHandlerConfig createDefault() {
        return new ErrorHandlerConfig();
    }
    
    /**
     * Creates a configuration optimized for development environments
     * with maximum detail and minimal suppression
     */
    public static ErrorHandlerConfig createDevelopmentConfig() {
        ErrorHandlerConfig config = new ErrorHandlerConfig();
        config.setMaxErrorRepetitions(10);
        config.setSuppressionTimeMinutes(5);
        config.setDetailedLogging(true);
        config.setLogStackTraces(true);
        config.setCategorizeErrors(true);
        config.setGroupSimilarErrors(false); // Show all errors in development
        return config;
    }
    
    /**
     * Creates a configuration optimized for production environments
     * with moderate detail and stronger suppression
     */
    public static ErrorHandlerConfig createProductionConfig() {
        ErrorHandlerConfig config = new ErrorHandlerConfig();
        config.setMaxErrorRepetitions(2);
        config.setSuppressionTimeMinutes(30);
        config.setDetailedLogging(false);
        config.setLogStackTraces(true);
        config.setCategorizeErrors(true);
        config.setGroupSimilarErrors(true);
        return config;
    }
}