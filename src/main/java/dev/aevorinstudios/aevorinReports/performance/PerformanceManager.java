package dev.aevorinstudios.aevorinReports.performance;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.aevorinstudios.aevorinReports.config.ConfigManager;
import dev.aevorinstudios.aevorinReports.model.Report;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class PerformanceManager {
    private final ConfigManager configManager;
    private Cache<Long, Report> reportCache;
    private ExecutorService asyncExecutor;
    private static final int THREAD_POOL_SIZE = 3;

    public void initialize() {
        initializeCache();
        initializeAsyncExecutor();
        startBackgroundTasks();
    }

    private void initializeCache() {
        if (configManager.getConfig().getPerformance().isEnableCaching()) {
            reportCache = CacheBuilder.newBuilder()
                    .maximumSize(configManager.getConfig().getPerformance().getMaxCacheSize())
                    .expireAfterWrite(configManager.getConfig().getPerformance().getCacheDuration(), TimeUnit.MINUTES)
                    .build();
        }
    }

    private void initializeAsyncExecutor() {
        if (configManager.getConfig().getPerformance().isAsyncProcessing()) {
            asyncExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        }
    }

    private void startBackgroundTasks() {
        if (configManager.getConfig().getPerformance().isAsyncProcessing()) {
            int interval = configManager.getConfig().getPerformance().getBackgroundTaskInterval();
            // Schedule cache cleanup
            asyncExecutor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(interval * 1000L);
                        cleanupCache();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
    }

    public void submitAsyncTask(Runnable task) {
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.submit(task);
        } else {
            task.run();
        }
    }

    public <T> void processBatch(List<T> items, BatchProcessor<T> processor) {
        if (configManager.getConfig().getPerformance().isAsyncProcessing()) {
            int batchSize = configManager.getConfig().getPerformance().getBatchSize();
            for (int i = 0; i < items.size(); i += batchSize) {
                int end = Math.min(items.size(), i + batchSize);
                List<T> batch = items.subList(i, end);
                submitAsyncTask(() -> processor.processBatch(batch));
            }
        } else {
            processor.processBatch(items);
        }
    }

    public void cacheReport(Report report) {
        if (reportCache != null) {
            reportCache.put(report.getId(), report);
        }
    }

    public Report getCachedReport(long reportId) {
        return reportCache != null ? reportCache.getIfPresent(reportId) : null;
    }

    private void cleanupCache() {
        if (reportCache != null) {
            reportCache.cleanUp();
        }
    }

    public void shutdown() {
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
    }

    @FunctionalInterface
    public interface BatchProcessor<T> {
        void processBatch(List<T> batch);
    }
}