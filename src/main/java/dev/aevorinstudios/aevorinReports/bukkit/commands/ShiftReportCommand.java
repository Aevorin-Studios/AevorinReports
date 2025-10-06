package dev.aevorinstudios.aevorinReports.bukkit.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.reports.Report;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShiftReportCommand implements CommandExecutor {
    private final BukkitPlugin plugin;

    public ShiftReportCommand(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!player.hasPermission("aevorinreports.manage")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /shiftreport <id> <status>");
            return true;
        }

        try {
            long reportId = Long.parseLong(args[0]);
            String statusStr = args[1].toUpperCase();
            Report.ReportStatus newStatus = Report.ReportStatus.valueOf(statusStr);
            Report report = plugin.getDatabaseManager().getReport(reportId);
            if (report == null) {
                player.sendMessage(ChatColor.RED + "Report not found!");
                return true;
            }
            if (report.getStatus() == newStatus) {
                player.sendMessage(ChatColor.YELLOW + "Report is already in that category!");
                return true;
            }
            // Silently update without console messages
            report.setStatus(newStatus);
            plugin.getDatabaseManager().updateReport(report);
            
            // Send a discreet message to the player
            player.sendMessage(ChatColor.DARK_GRAY + "[Report System] " + ChatColor.GRAY + "ID: " + reportId + " → " + newStatus.name());
            
            // Log at FINE level (typically not shown in console unless debug is enabled)
            plugin.getLogger().fine("Report " + reportId + " status changed to " + newStatus.name() + " by " + player.getName());
            
            // Silently show updated details (avoid dispatching a visible command)
            new ReportManageGUI(plugin).open(player, report);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid report ID!");
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid status! Valid: " + java.util.Arrays.toString(Report.ReportStatus.values()));
        }
        return true;
    }
}
