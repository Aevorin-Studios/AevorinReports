package dev.aevorinstudios.aevorinReports.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.gui.ReportManageGUI;
import dev.aevorinstudios.aevorinReports.reports.Report;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SetReportStatusCommand implements CommandExecutor, TabCompleter {
    private final BukkitPlugin plugin;

    public SetReportStatusCommand(BukkitPlugin plugin) {
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

        if (args.length != 3 || !args[1].equalsIgnoreCase("to")) {
            player.sendMessage(ChatColor.RED + "Usage: /setreportstatus <id> to <status>");
            return true;
        }

        try {
            long reportId = Long.parseLong(args[0]);
            String statusStr = args[2].toUpperCase();
            Report.ReportStatus newStatus = Report.ReportStatus.valueOf(statusStr);
            Report report = plugin.getDatabaseManager().getReport(reportId);
            
            if (report == null) {
                player.sendMessage(ChatColor.RED + "Report not found!");
                return true;
            }
            
            if (report.getStatus() == newStatus) {
                player.sendMessage(ChatColor.YELLOW + "Report is already in that status!");
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
            player.sendMessage(ChatColor.RED + "Invalid status! Valid: " + Arrays.toString(Report.ReportStatus.values()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("aevorinreports.manage")) {
            return new ArrayList<>();
        }

        if (args.length == 2) {
            return List.of("to");
        } else if (args.length == 3 && args[1].equalsIgnoreCase("to")) {
            return Arrays.stream(Report.ReportStatus.values())
                    .map(Enum::name)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}