package dev.aevorinstudios.aevorinReports.reports;

import lombok.Data;
import lombok.Builder;
import org.bukkit.configuration.ConfigurationSection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class Report {
    private Long id;
    private UUID reporterUuid;
    private UUID reportedUuid;
    private String reporterName;
    private String reportedPlayerName;
    private String reason;
    private String serverName;
    private ReportStatus status;
    private String lastUpdatedBy;
    private boolean isAnonymous;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String evidenceData;
    private String coordinates;
    private String world;

    public enum ReportStatus {
        PENDING,
        RESOLVED,
        REJECTED
    }

    public boolean canTransitionTo(ReportStatus newStatus, ConfigurationSection statusConfig) {
        if (this.status == newStatus) return false;

        String currentStatusKey = this.status.name().toLowerCase();
        ConfigurationSection currentStatusSection = statusConfig.getConfigurationSection("types." + currentStatusKey);
        
        if (currentStatusSection == null) return false;
        
        List<String> allowedTransitions = currentStatusSection.getStringList("transitions");
        return allowedTransitions.contains(newStatus.name().toLowerCase());
    }

    public boolean isActive() {
        return status == ReportStatus.PENDING;
    }

    public boolean canBeUpdated() {
        return status != ReportStatus.RESOLVED && status != ReportStatus.REJECTED;
    }

    public UUID getReporter() {
        return reporterUuid;
    }

    public UUID getReported() {
        return reportedUuid;
    }

    public String getCategory() {
        return reason;
    }
}