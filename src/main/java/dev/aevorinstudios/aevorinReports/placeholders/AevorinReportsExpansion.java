package dev.aevorinstudios.aevorinReports.placeholders;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.database.DatabaseManager;
import dev.aevorinstudios.aevorinReports.reports.Report;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI Expansion for AevorinReports
 * Provides player report statistics as placeholders
 */
public class AevorinReportsExpansion extends PlaceholderExpansion {

    private final BukkitPlugin plugin;

    public AevorinReportsExpansion(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "aevorinreports";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    @Nullable
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return null;
        }

        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) {
            return "0";
        }

        switch (identifier.toLowerCase()) {
            // Reports submitted by the player (as reporter)
            case "reports_submitted":
            case "submitted":
                return String.valueOf(db.getReportsCountByReporter(player.getUniqueId()));

            case "reports_submitted_pending":
            case "submitted_pending":
                return String.valueOf(db.getReportsCountByReporterAndStatus(player.getUniqueId(), Report.ReportStatus.PENDING));

            case "reports_submitted_resolved":
            case "submitted_resolved":
                return String.valueOf(db.getReportsCountByReporterAndStatus(player.getUniqueId(), Report.ReportStatus.RESOLVED));

            case "reports_submitted_rejected":
            case "submitted_rejected":
                return String.valueOf(db.getReportsCountByReporterAndStatus(player.getUniqueId(), Report.ReportStatus.REJECTED));

            case "reports_submitted_valid":
            case "submitted_valid":
                // Valid reports = resolved reports (reports that were acted upon)
                return String.valueOf(db.getReportsCountByReporterAndStatus(player.getUniqueId(), Report.ReportStatus.RESOLVED));

            // Reports received against the player (as reported)
            case "reports_received":
            case "received":
                return String.valueOf(db.getReportsCountByReported(player.getUniqueId()));

            case "reports_received_pending":
            case "received_pending":
                return String.valueOf(db.getReportsCountByReportedAndStatus(player.getUniqueId(), Report.ReportStatus.PENDING));

            case "reports_received_resolved":
            case "received_resolved":
                return String.valueOf(db.getReportsCountByReportedAndStatus(player.getUniqueId(), Report.ReportStatus.RESOLVED));

            case "reports_received_rejected":
            case "received_rejected":
                return String.valueOf(db.getReportsCountByReportedAndStatus(player.getUniqueId(), Report.ReportStatus.REJECTED));

            // Server-wide statistics (player-independent)
            case "total_reports":
                return String.valueOf(db.getTotalReportsCount());

            case "pending_reports":
            case "total_pending":
                return String.valueOf(db.getReportCountByStatus(Report.ReportStatus.PENDING));

            case "resolved_reports":
            case "total_resolved":
                return String.valueOf(db.getReportCountByStatus(Report.ReportStatus.RESOLVED));

            case "rejected_reports":
            case "total_rejected":
                return String.valueOf(db.getReportCountByStatus(Report.ReportStatus.REJECTED));

            default:
                return null;
        }
    }
}
