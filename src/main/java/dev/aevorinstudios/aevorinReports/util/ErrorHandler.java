package dev.aevorinstudios.aevorinReports.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced error handling system for AevorinReports plugin.
 * Provides structured error reporting, rate limiting, and context-aware logging.
 */
public class ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
    private static ErrorHandler instance;
    
    // Rate limiting for similar errors
    private final Map<String, ErrorOccurrence> errorOccurrences = new ConcurrentHashMap<>();
    private final Map<String, Long> errorSuppression = new ConcurrentHashMap<>();
    
    // Configuration
    private int maxErrorRepetitions = 3;
    private long suppressionTimeMinutes = 10;
    private boolean detailedLogging = true;
    private boolean logStackTraces = true;
    
    private ErrorHandler() {
        // Private constructor for singleton
    }
    
    public static synchronized ErrorHandler getInstance() {
        if (instance == null) {
            instance = new ErrorHandler();
        }
        return instance;
    }
    
    /**
     * Handles an exception with context information
     * 
     * @param e The exception to handle
     * @param source The source/component where the exception occurred
     * @param context Additional context information
     */
    public void handleException(Throwable e, String source, Map<String, Object> context) {
        if (e == null) return;
        
        // Generate error signature for rate limiting
        String errorSignature = generateErrorSignature(e, source);
        
        // Check if this error is currently suppressed
        if (isErrorSuppressed(errorSignature)) {
            return;
        }
        
        // Format the error message with context
        String formattedMessage = formatErrorMessage(e, source, context);
        
        // Track error occurrence for rate limiting
        ErrorOccurrence occurrence = errorOccurrences.computeIfAbsent(errorSignature, 
                k -> new ErrorOccurrence());
        occurrence.count++;
        occurrence.lastOccurrence = System.currentTimeMillis();
        
        // Log the error with appropriate level
        if (occurrence.count <= maxErrorRepetitions) {
            if (e instanceof RuntimeException || e instanceof Error) {
                logger.error(formattedMessage, e);
            } else {
                logger.warn(formattedMessage, e);
            }
        } else if (occurrence.count == maxErrorRepetitions + 1) {
            // Log that we're suppressing this error
            logger.warn("Suppressing further occurrences of error for {} minutes: {}", 
                    suppressionTimeMinutes, errorSignature);
            errorSuppression.put(errorSignature, 
                    System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(suppressionTimeMinutes));
        }
    }
    
    /**
     * Simplified method to handle an exception with minimal context
     */
    public void handleException(Throwable e, String source) {
        handleException(e, source, new HashMap<>());
    }
    
    /**
     * Formats an error message with context information
     */
    private String formatErrorMessage(Throwable e, String source, Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("[AevorinReports] Error in component: ").append(source).append("\n");
        sb.append("Type: ").append(e.getClass().getName()).append("\n");
        sb.append("Message: ").append(e.getMessage()).append("\n");
        
        if (!context.isEmpty() && detailedLogging) {
            sb.append("Context:\n");
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ");
                if (entry.getValue() != null) {
                    sb.append(entry.getValue().toString());
                } else {
                    sb.append("null");
                }
                sb.append("\n");
            }
        }
        
        if (logStackTraces) {
            sb.append("Stack trace:\n").append(getStackTraceAsString(e));
        }
        
        return sb.toString();
    }
    
    /**
     * Converts a stack trace to a string
     */
    private String getStackTraceAsString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Generates a signature for an error to identify similar errors
     */
    private String generateErrorSignature(Throwable e, String source) {
        return source + "-" + e.getClass().getName() + "-" + 
                (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "unknown");
    }
    
    /**
     * Checks if an error is currently suppressed
     */
    private boolean isErrorSuppressed(String errorSignature) {
        Long suppressUntil = errorSuppression.get(errorSignature);
        if (suppressUntil != null) {
            if (System.currentTimeMillis() < suppressUntil) {
                return true;
            } else {
                // Suppression period has ended
                errorSuppression.remove(errorSignature);
                errorOccurrences.remove(errorSignature);
            }
        }
        return false;
    }
    
    /**
     * Installs a global uncaught exception handler
     */
    public void installGlobalHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Map<String, Object> context = new HashMap<>();
            context.put("thread", thread.getName());
            context.put("thread_id", thread.getId());
            handleException(throwable, "UncaughtExceptionHandler", context);
        });
        logger.info("Installed global exception handler");
    }
    
    /**
     * Configure the error handler
     */
    public void configure(int maxRepetitions, long suppressionMinutes, boolean detailed, boolean stackTraces) {
        this.maxErrorRepetitions = maxRepetitions;
        this.suppressionTimeMinutes = suppressionMinutes;
        this.detailedLogging = detailed;
        this.logStackTraces = stackTraces;
    }
    
    /**
     * Tracks occurrences of a specific error
     */
    private static class ErrorOccurrence {
        int count = 0;
        long lastOccurrence = 0;
    }
}