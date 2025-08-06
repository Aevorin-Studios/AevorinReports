package dev.aevorinstudios.aevorinReports.reports;

import java.util.UUID;

public class Report {
    private UUID id;
    private String reporterName;
    private String reportedPlayerName;
    private String reason;
    private String serverName;
    private ReportStatus status;
    private String lastUpdatedBy;

    public Report(String reporterName, String reportedPlayerName, String reason, String serverName) {
        this.id = UUID.randomUUID();
        this.reporterName = reporterName;
        this.reportedPlayerName = reportedPlayerName;
        this.reason = reason;
        this.serverName = serverName;
        this.status = ReportStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public String getReporterName() {
        return reporterName;
    }

    public String getReportedPlayerName() {
        return reportedPlayerName;
    }

    public String getReason() {
        return reason;
    }

    public String getServerName() {
        return serverName;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }
}