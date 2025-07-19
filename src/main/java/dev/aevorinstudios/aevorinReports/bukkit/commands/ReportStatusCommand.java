package dev.aevorinstudios.aevorinReports.bukkit.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.model.Report;
import dev.aevorinstudios.aevorinReports.notification.NotificationManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ReportStatusCommand implements CommandExecutor {
    private final BukkitPlugin plugin;
    private final Map<String, Report.ReportStatus> statusMap;
    private final Map<Report.ReportStatus, String> statusColors;

    public ReportStatusCommand(BukkitPlugin plugin) {
        this.plugin = plugin;
        this.statusMap = new HashMap<>();
        this.statusColors = new HashMap<>();
        initializeStatusMaps();
    }

    private void initializeStatusMaps() {
        ConfigurationSection statusSection = plugin.getConfig().getConfigurationSection("reports.status.types");
        if (statusSection == null) return;

        for (String key : statusSection.getKeys(false)) {
            if (key.equalsIgnoreCase("IN_PROGRESS")) continue;

            String statusName = statusSection.getString(key + ".name");
            String statusColor = statusSection.getString(key + ".color", "&f");
            Report.ReportStatus status;

            try {
                status = Report.ReportStatus.valueOf(key.toUpperCase());
                statusMap.put(statusName.toLowerCase(), status);
                statusColors.put(status, statusColor);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aevorinreports.manage")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to do that!")));
            return true;
        }

        ConfigurationSection statusConfig = plugin.getConfig().getConfigurationSection("reports.status");
        if (statusConfig == null) {
            sender.sendMessage(ChatColor.RED + "Status configuration is missing!");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /reportstatus <id> <status>");
            return true;
        }

        long reportId;
        try {
            reportId = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid report ID!");
            return true;
        }

        String statusArg = args[1].toLowerCase();
        Report.ReportStatus newStatus = statusMap.get(statusArg);

        if (newStatus == null) {
            String availableStatuses = String.join(", ", statusMap.keySet());
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.report-status-invalid", "&cInvalid report status! Available statuses: {statuses}")
                    .replace("{statuses}", availableStatuses)));
            return true;
        }

        Report report = plugin.getDatabaseManager().getReport(reportId);
        if (report == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.report-not-found", "&cReport not found!")));
            return true;
        }

        ConfigurationSection newStatusSection = statusConfig.getConfigurationSection("types." + statusArg);
        if (newStatusSection == null) {
            sender.sendMessage(ChatColor.RED + "Invalid status configuration!");
            return true;
        }

        String requiredPermission = newStatusSection.getString("permission");
        if (requiredPermission != null && !sender.hasPermission(requiredPermission)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to do that!")));
            return true;
        }

        if (!report.canTransitionTo(newStatus, statusConfig)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.invalid-status-transition", "&cInvalid status transition!")
                    .replace("{current}", report.getStatus().toString())
                    .replace("{status}", newStatus.toString())));
            return true;
        }

        updateReportStatus(sender, reportId, newStatus);
        return true;
    }

    private void updateReportStatus(CommandSender sender, long reportId, Report.ReportStatus newStatus) {
        Report report = plugin.getDatabaseManager().getReport(reportId);
        if (report == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.report-not-found", "&cReport not found!")));
            return;
        }

        Report updatedReport = Report.builder()
                .id(report.getId())
                .reporterUuid(report.getReporter())
                .reportedUuid(report.getReported())
                .reason(report.getCategory())
                .serverName(report.getServerName())
                .status(newStatus)
                .isAnonymous(report.isAnonymous())
                .createdAt(report.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .evidenceData(report.getEvidenceData())
                .coordinates(report.getCoordinates())
                .world(report.getWorld())
                .build();

        try {
            plugin.getDatabaseManager().updateReport(updatedReport);
            
            // Send a success message to command sender
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.report-status-update-success", "&aReport status updated successfully!")));

            // Send status change notification if enabled
            if (plugin.getConfig().getBoolean("notifications.status-change", true)) {
                String statusColor = statusColors.getOrDefault(newStatus, "&f");
                String notificationMessage = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.report-status-change", "&7Report #{id} status has been changed to {color}{status}&7.")
                        .replace("{id}", String.valueOf(reportId))
                        .replace("{color}", statusColor)
                        .replace("{status}", newStatus.toString()));

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.hasPermission("aevorinreports.notify")) {
                        player.sendMessage(notificationMessage);
                    }
                }
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.report-status-update-failed", "&cFailed to update report status!")));
            e.printStackTrace();
        }
    }
}