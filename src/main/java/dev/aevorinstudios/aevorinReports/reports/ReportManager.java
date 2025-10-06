package dev.aevorinstudios.aevorinReports.reports;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

public class ReportManager {
    private static ReportManager instance;
    private final Map<Long, Report> reports;

    private ReportManager() {
        this.reports = new ConcurrentHashMap<>();
    }

    public static ReportManager getInstance() {
        if (instance == null) {
            instance = new ReportManager();
        }
        return instance;
    }

    public Report createReport(UUID reporterId, String reporterName, UUID reportedId, String reportedName, String description, String serverName) {
        Report report = Report.builder()
            .id(System.currentTimeMillis())
            .reporterUuid(reporterId)
            .reportedUuid(reportedId)
            .reporterName(reporterName)
            .reportedPlayerName(reportedName)
            .reason(description)
            .serverName(serverName)
            .status(Report.ReportStatus.PENDING)
            .isAnonymous(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        reports.put(report.getId(), report);
        return report;
    }

    public Report getReport(Long reportId) {
        return reports.get(reportId);
    }

    public boolean updateReportStatus(Long reportId, Report.ReportStatus newStatus) {
        Report report = reports.get(reportId);
        if (report != null) {
            report.setStatus(newStatus);
            return true;
        }
        return false;
    }

    public void deleteReport(Long reportId) {
        reports.remove(reportId);
    }

    public Map<Long, Report> getAllReports() {
        return new HashMap<>(reports);
    }

    public void clearReports() {
        reports.clear();
    }
}