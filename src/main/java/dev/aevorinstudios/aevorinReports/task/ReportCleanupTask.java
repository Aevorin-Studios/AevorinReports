package dev.aevorinstudios.aevorinReports.task;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.model.Report;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ReportCleanupTask extends BukkitRunnable {
    private final BukkitPlugin plugin;

    public ReportCleanupTask(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        ConfigurationSection autoCloseConfig = plugin.getConfig().getConfigurationSection("reports.status.auto_close");
        if (autoCloseConfig == null || !autoCloseConfig.getBoolean("enabled", true)) {
            return;
        }

        int resolvedDeleteAfter = autoCloseConfig.getInt("resolved_delete_after", 168);
        int rejectedDeleteAfter = autoCloseConfig.getInt("rejected_delete_after", 72);

        // Delete resolved reports
        LocalDateTime resolvedThreshold = LocalDateTime.now().minus(resolvedDeleteAfter, ChronoUnit.HOURS);
        plugin.getDatabaseManager().deleteOldReports(Report.ReportStatus.RESOLVED, resolvedThreshold);

        // Delete rejected reports
        LocalDateTime rejectedThreshold = LocalDateTime.now().minus(rejectedDeleteAfter, ChronoUnit.HOURS);
        plugin.getDatabaseManager().deleteOldReports(Report.ReportStatus.REJECTED, rejectedThreshold);
    }

    public void schedule() {
        // Run task every hour
        this.runTaskTimerAsynchronously(plugin, 20L * 60L * 60L, 20L * 60L * 60L);
    }
}