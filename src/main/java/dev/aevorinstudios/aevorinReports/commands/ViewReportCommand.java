package dev.aevorinstudios.aevorinReports.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.utils.PlayerNameResolver;
import dev.aevorinstudios.aevorinReports.gui.BookGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ViewReportCommand implements CommandExecutor {
    private final BukkitPlugin plugin;

    public ViewReportCommand(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        // Permission check moved to after report retrieval to allow self-viewing


        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /viewreport <id>");
            return true;
        }

        try {
            long reportId = Long.parseLong(args[0]);
            showReportDetails(player, reportId);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid report ID!");
        }

        return true;
    }

    private void showReportDetails(Player player, long reportId) {
        Report report = plugin.getDatabaseManager().getReport(reportId);
        if (report == null) {
            player.sendMessage(ChatColor.RED + "Report not found!");
            return;
        }

        // Permission check: Allow if has permission OR is the reporter
        if (!player.hasPermission("aevorinreports.manage") && !report.getReporterUuid().equals(player.getUniqueId())) {
             player.sendMessage(ChatColor.RED + "You don't have permission to view this report!");
             return;
        }

        new dev.aevorinstudios.aevorinReports.gui.ReportManageGUI(plugin).open(player, report);
    }
}