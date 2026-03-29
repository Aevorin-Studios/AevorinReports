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

import dev.aevorinstudios.aevorinReports.config.LanguageManager;

public class ViewReportCommand implements CommandExecutor {
    private final BukkitPlugin plugin;

    public ViewReportCommand(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = LanguageManager.get(plugin);
        if (!(sender instanceof Player player)) {
            dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(sender, lang.getMessage("messages.error.player-only"));
            return true;
        }

        // Permission check moved to after report retrieval to allow self-viewing


        if (args.length != 1) {
            dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(player, lang.getMessage("messages.error.usage-viewreport"));
            return true;
        }

        try {
            long reportId = Long.parseLong(args[0]);
            showReportDetails(player, reportId);
        } catch (NumberFormatException e) {
            dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(player, lang.getMessage("messages.error.invalid-report-id"));
        }

        return true;
    }

    private void showReportDetails(Player player, long reportId) {
        Report report = plugin.getDatabaseManager().getReport(reportId);
        if (report == null) {
            dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(player, dev.aevorinstudios.aevorinReports.config.LanguageManager.get(plugin).getMessage("messages.error.report-not-found"));
            return;
        }

        // Permission check: Allow if has permission OR is the reporter
        if (!player.hasPermission("aevorinreports.manage") && !report.getReporterUuid().equals(player.getUniqueId())) {
             dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(player, dev.aevorinstudios.aevorinReports.config.LanguageManager.get(plugin).getMessage("messages.error.no-permission"));
             return;
        }

        new dev.aevorinstudios.aevorinReports.gui.ReportManageGUI(plugin).open(player, report);
    }
}