package dev.aevorinstudios.aevorinReports.storage;

import dev.aevorinstudios.aevorinReports.config.Settings;
import dev.aevorinstudios.aevorinReports.database.DatabaseManager;
import dev.aevorinstudios.aevorinReports.reports.Report;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class StorageManager {
    private final Settings settings;
    private final DatabaseManager databaseManager;
    private final ScheduledExecutorService scheduler;

    public void initialize() {
        if (settings.getStorage().getKeepResolvedReports() > 0 ||
            settings.getStorage().getKeepRejectedReports() > 0) {
            scheduleCleanupTask();
        }
    }

    private void scheduleCleanupTask() {
        scheduler.scheduleAtFixedRate(
            this::cleanupOldReports,
            1,
            24,
            TimeUnit.HOURS
        );
    }

    private void cleanupOldReports() {
        int resolvedDays = settings.getStorage().getKeepResolvedReports();
        int rejectedDays = settings.getStorage().getKeepRejectedReports();

        if (resolvedDays > 0) {
            LocalDateTime resolvedCutoff = LocalDateTime.now().minus(resolvedDays, ChronoUnit.DAYS);
            List<Report> resolvedReports = databaseManager.getResolvedReportsBefore(resolvedCutoff);
            for (Report report : resolvedReports) {
                databaseManager.deleteReport(report.getId());
            }
        }

        if (rejectedDays > 0) {
            LocalDateTime rejectedCutoff = LocalDateTime.now().minus(rejectedDays, ChronoUnit.DAYS);
            List<Report> rejectedReports = databaseManager.getRejectedReportsBefore(rejectedCutoff);
            for (Report report : rejectedReports) {
                databaseManager.deleteReport(report.getId());
            }
        }
    }
}