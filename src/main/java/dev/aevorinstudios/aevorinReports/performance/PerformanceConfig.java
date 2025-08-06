package dev.aevorinstudios.aevorinReports.performance;

import dev.aevorinstudios.aevorinReports.config.Settings;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

@Getter
public class PerformanceConfig {
    private final boolean cachingEnabled;
    private final long cacheDuration;
    private final int maxCacheSize;
    private final boolean asyncProcessingEnabled;
    private final int batchSize;
    private final long backgroundTaskInterval;
    private final long cacheCleanupInterval;

    public PerformanceConfig(Settings settings) {
        Settings.PerformanceSettings performanceSettings = settings.getPerformance();
        this.cachingEnabled = performanceSettings.isEnableCaching();
        this.cacheDuration = TimeUnit.MINUTES.toMillis(performanceSettings.getCacheDuration());
        this.maxCacheSize = performanceSettings.getMaxCacheSize();
        this.asyncProcessingEnabled = performanceSettings.isAsyncProcessing();
        this.batchSize = performanceSettings.getBatchSize();
        this.backgroundTaskInterval = TimeUnit.SECONDS.toMillis(performanceSettings.getBackgroundTaskInterval());
        this.cacheCleanupInterval = TimeUnit.SECONDS.toMillis(performanceSettings.getCacheCleanupInterval());
    }

    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    public boolean isAsyncProcessingEnabled() {
        return asyncProcessingEnabled;
    }
}