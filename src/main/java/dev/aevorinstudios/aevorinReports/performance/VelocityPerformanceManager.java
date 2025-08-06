package dev.aevorinstudios.aevorinReports.performance;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.aevorinstudios.aevorinReports.config.VelocityConfigManager;

import java.util.concurrent.*;
import java.util.logging.Logger;

public class VelocityPerformanceManager {
    private final ProxyServer server;
    private final VelocityConfigManager config;
    private final Logger logger;
    private Cache<String, Object> reportCache;
    private ExecutorService asyncExecutor;
    private BlockingQueue<Runnable> asyncQueue;

    public VelocityPerformanceManager(ProxyServer server, VelocityConfigManager config, Logger logger) {
        this.server = server;
        this.config = config;
        this.logger = logger;
        initializeCache();
        initializeAsyncProcessing();
    }

    private void initializeCache() {
        if (config.isCacheEnabled()) {
            reportCache = CacheBuilder.newBuilder()
                    .maximumSize(config.getMaxCachedReports())
                    .expireAfterWrite(config.getCacheExpiration(), TimeUnit.MINUTES)
                    .build();
            logger.info("Report cache initialized with size " + config.getMaxCachedReports());
        }
    }

    private void initializeAsyncProcessing() {
        if (config.isAsyncProcessingEnabled()) {
            asyncQueue = new LinkedBlockingQueue<>(config.getMaxAsyncQueueSize());
            asyncExecutor = new ThreadPoolExecutor(
                    config.getAsyncThreadPoolSize(),
                    config.getAsyncThreadPoolSize(),
                    60L, TimeUnit.SECONDS,
                    asyncQueue,
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
            logger.info("Async processing initialized with " + config.getAsyncThreadPoolSize() + " threads");
        }
    }

    public void submitAsyncTask(Runnable task) {
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            try {
                asyncExecutor.submit(task);
            } catch (RejectedExecutionException e) {
                logger.warning("Async task rejected: Queue full");
                task.run(); // Fallback to sync execution
            }
        } else {
            task.run(); // Fallback to sync execution if async processing is disabled
        }
    }

    public void putInCache(String key, Object value) {
        if (reportCache != null) {
            reportCache.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getFromCache(String key) {
        return reportCache != null ? (T) reportCache.getIfPresent(key) : null;
    }

    public void invalidateCache(String key) {
        if (reportCache != null) {
            reportCache.invalidate(key);
        }
    }

    public void cleanup() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (reportCache != null) {
            reportCache.invalidateAll();
        }
    }
}