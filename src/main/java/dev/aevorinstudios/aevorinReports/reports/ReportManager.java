package dev.aevorinstudios.aevorinReports.reports;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReportManager {
    private static ReportManager instance;
    private final Map<UUID, Report> reports;

    private ReportManager() {
        this.reports = new ConcurrentHashMap<>();
    }

    public static ReportManager getInstance() {
        if (instance == null) {
            instance = new ReportManager();
        }
        return instance;
    }

    public Report createReport(UUID reporterId, String description) {
        Report report = new Report("ReporterName", reporterId.toString(), "ReportedPlayerName", description);
        reports.put(report.getId(), report);
        return report;
    }

    public Report getReport(UUID reportId) {
        return reports.get(reportId);
    }

    public boolean updateReportStatus(UUID reportId, ReportStatus newStatus) {
        Report report = reports.get(reportId);
        if (report != null) {
            report.setStatus(newStatus);
            return true;
        }
        return false;
    }

    public void deleteReport(UUID reportId) {
        reports.remove(reportId);
    }

    public Map<UUID, Report> getAllReports() {
        return new HashMap<>(reports);
    }

    public void clearReports() {
        reports.clear();
    }
}