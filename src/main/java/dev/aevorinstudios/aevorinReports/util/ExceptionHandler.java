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
 * Provides structured error reporting, rate limiting, and context-aware logging
 * to prevent console flooding while providing detailed diagnostic information.
 */
public class ExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);
    private static ExceptionHandler instance;
    
    // Rate limiting for similar errors
    private final Map<String, ErrorOccurrence> errorOccurrences = new ConcurrentHashMap<>();
    private final Map<String, Long> errorSuppression = new ConcurrentHashMap<>();
    
    // Configuration
    private int maxErrorRepetitions = 3;
    private long suppressionTimeMinutes = 10;
    private boolean detailedLogging = true;
    private boolean logStackTraces = true;
    private boolean groupSimilarErrors = true;
    
    private ExceptionHandler() {
        // Private constructor for singleton
    }
    
    public static synchronized ExceptionHandler getInstance() {
        if (instance == null) {
            instance = new ExceptionHandler();
        }
        return instance;
    }
    
    /**
     * Installs a global uncaught exception handler to catch all unhandled exceptions
     * in the plugin
     */
    public void installGlobalHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Map<String, Object> context = new HashMap<>();
            context.put("thread", thread.getName());
            context.put("thread_id", thread.getId());
            handleException(throwable, "UncaughtExceptionHandler", context);
        });
        logger.info("Installed global exception handler for AevorinReports");
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
        
        // Log the error with appropriate level and rate limiting
        if (occurrence.count <= maxErrorRepetitions) {
            if (e instanceof RuntimeException || e instanceof Error) {
                logger.error(formattedMessage);
                if (logStackTraces) {
                    logger.error("Stack trace:", e);
                }
            } else {
                logger.warn(formattedMessage);
                if (logStackTraces) {
                    logger.warn("Stack trace:", e);
                }
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
        sb.append("Message: ").append(e.getMessage() != null ? e.getMessage() : "<no message>").append("\n");
        
        // Add root cause if available
        Throwable rootCause = getRootCause(e);
        if (rootCause != e) {
            sb.append("Root cause: ").append(rootCause.getClass().getName());
            if (rootCause.getMessage() != null) {
                sb.append(" - ").append(rootCause.getMessage());
            }
            sb.append("\n");
        }
        
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
        
        return sb.toString();
    }
    
    /**
     * Gets the root cause of an exception
     */
    private Throwable getRootCause(Throwable e) {
        Throwable cause = e.getCause();
        if (cause == null) {
            return e;
        }
        return getRootCause(cause);
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
        if (!groupSimilarErrors) {
            // Use a more specific signature if we're not grouping similar errors
            return source + "-" + e.getClass().getName() + "-" + 
                    (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "unknown") +
                    "-" + System.currentTimeMillis();
        }
        
        // Group similar errors by class, source and first stack trace element
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
     * Configure the error handler
     */
    public void configure(int maxRepetitions, long suppressionMinutes, boolean detailed, boolean stackTraces) {
        this.maxErrorRepetitions = maxRepetitions;
        this.suppressionTimeMinutes = suppressionMinutes;
        this.detailedLogging = detailed;
        this.logStackTraces = stackTraces;
    }
    
    /**
     * Set whether to group similar errors
     */
    public void setGroupSimilarErrors(boolean groupSimilarErrors) {
        this.groupSimilarErrors = groupSimilarErrors;
    }
    
    /**
     * Tracks occurrences of a specific error
     */
    private static class ErrorOccurrence {
        int count = 0;
        long lastOccurrence = 0;
    }
}