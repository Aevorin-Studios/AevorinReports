package dev.aevorinstudios.aevorinReports.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.database.DatabaseManager;
import dev.aevorinstudios.aevorinReports.gui.BookGUI;
import dev.aevorinstudios.aevorinReports.gui.ReportReasonContainerGUI;
import dev.aevorinstudios.aevorinReports.reports.Report;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.Material;
import net.md_5.bungee.api.ChatColor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BukkitReportCommand implements CommandExecutor, TabCompleter {
    private final BukkitPlugin plugin;

    public BukkitReportCommand(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!player.hasPermission("aevorinreports.report")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /report <player> [reason]");
            return true;
        }

        String targetPlayer = args[0];
        Player target = plugin.getServer().getPlayer(targetPlayer);
        
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        // Self-reporting check
        boolean allowSelfReporting = plugin.getConfig().getBoolean("reports.allow-self-reporting", true);
        if (!allowSelfReporting && player.getName().equalsIgnoreCase(targetPlayer)) {
            player.sendMessage(ChatColor.RED + "You cannot report yourself.");
            return true;
        }

        // If reason is provided, create report directly
        if (args.length > 1) {
            String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            
            // Handle custom reason
            if (reason.equalsIgnoreCase("custom")) {
                if (!plugin.getConfig().getBoolean("reports.allow-custom-reasons", true)) {
                    dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(player, plugin.getConfig().getString("messages.custom-reason-disabled", "Custom reasons are disabled."));
                    return true;
                }
                
                // Store player and target info for custom reason handling
                plugin.getCustomReasonHandler().startCustomReason(player, targetPlayer);
                dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(player, plugin.getConfig().getString("messages.enter-custom-reason", "Please enter your reason in the chat:"));
                return true;
            }
            
            createReport(player, targetPlayer, reason);
            return true;
        }

        // If no reason provided, show GUI
        showReportCategories(player, target.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("aevorinreports.report")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            String partialName = args[0].toLowerCase();
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialName)) {
                    playerNames.add(player.getName());
                }
            }
            
            return playerNames;
        } else if (args.length == 2) {
            List<String> suggestions = new ArrayList<>(plugin.getConfig().getStringList("reports.categories"));
            if (plugin.getConfig().getBoolean("reports.allow-custom-reasons", true)) {
                suggestions.add("custom");
            }
            
            String partialReason = args[1].toLowerCase();
            return suggestions.stream()
                .filter(reason -> reason.toLowerCase().startsWith(partialReason))
                .toList();
        }
        
        return new ArrayList<>();
    }

    public void createReport(Player reporter, String targetPlayer, String category) {
        LocalDateTime now = LocalDateTime.now();
        Report report = Report.builder()
                .reporterUuid(reporter.getUniqueId())
                .reportedUuid(plugin.getServer().getOfflinePlayer(targetPlayer).getUniqueId())
                .reason(category)
                .serverName(plugin.getServer().getName())
                .status(Report.ReportStatus.PENDING)
                .isAnonymous(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Save report to database
        DatabaseManager.getInstance().saveReport(report);
        // The report ID is set by the saveReport method

        // Notify staff members with permission
        String notificationFormat = plugin.getConfig().getString("messages.report-notification", "&b{reporter} &7has reported &b{reported} &7for &b{category}&7.");
        String notification = notificationFormat
                .replace("{reporter}", reporter.getName())
                .replace("{reported}", targetPlayer)
                .replace("{category}", category);
        
        for (Player staff : plugin.getServer().getOnlinePlayers()) {
            if (staff.hasPermission("aevorinreports.notify")) {
                dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(staff, notification);
            }
        }
        
        // Notify reporter of success
        dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(reporter, plugin.getConfig().getString("messages.report-created", "&aYour report has been submitted successfully!"));
    }

    private void showReportCategories(Player player, String targetPlayer) {
        String guiType = plugin.getConfig().getString("reports.gui.type", "book");
        
        if (guiType.equalsIgnoreCase("container")) {
            // Use container GUI
            new ReportReasonContainerGUI(plugin).showReasonContainerGUI(player, targetPlayer);
        } else {
            // Default to book GUI
            showReportBookGUI(player, targetPlayer);
        }
    }
    
    private void showReportBookGUI(Player player, String targetPlayer) {
        new BookGUI(plugin).showReportCategories(player, targetPlayer);
    }
}