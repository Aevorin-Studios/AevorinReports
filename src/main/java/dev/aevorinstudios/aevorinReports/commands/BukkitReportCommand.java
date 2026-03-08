package dev.aevorinstudios.aevorinReports.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.database.DatabaseManager;
import dev.aevorinstudios.aevorinReports.gui.BookGUI;
import dev.aevorinstudios.aevorinReports.gui.ReportReasonContainerGUI;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.config.LanguageManager;
import dev.aevorinstudios.aevorinReports.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatColor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BukkitReportCommand implements CommandExecutor, TabCompleter {
    private final BukkitPlugin plugin;
    private final Map<java.util.UUID, Long> cooldowns = new HashMap<>();

    public BukkitReportCommand(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = LanguageManager.get(plugin);
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!player.hasPermission("aevorinreports.report")) {
            MessageUtils.sendMessage(player, lang.getMessage("messages.error.no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /report <player> [reason]");
            return true;
        }

        String targetPlayer = args[0];
        Player target = plugin.getServer().getPlayer(targetPlayer);

        if (target == null) {
            MessageUtils.sendMessage(player, lang.getMessage("messages.error.invalid-player"));
            return true;
        }

        // Self-reporting check
        boolean allowSelfReporting = plugin.getConfig().getBoolean("reports.allow-self-reporting", false);
        if (!allowSelfReporting && player.getName().equalsIgnoreCase(targetPlayer)) {
            player.sendMessage(ChatColor.RED + "You cannot report yourself.");
            return true;
        }

        // Check report cooldown
        if (!player.hasPermission("aevorinreports.bypass.cooldown")) {
            long lastReport = cooldowns.getOrDefault(player.getUniqueId(), 0L);
            int cooldownSeconds = plugin.getConfigManager().getConfig().getReports().getCooldownSeconds();
            long currentTime = System.currentTimeMillis();

            if (currentTime < lastReport + (cooldownSeconds * 1000L)) {
                long timeLeft = (lastReport + (cooldownSeconds * 1000L) - currentTime) / 1000L;
                String message = lang.getMessage("messages.report.cooldown", Map.of("time", formatTime(timeLeft)));
                MessageUtils.sendMessage(player, message);
                return true;
            }
        }

        // Check active reports limit
        if (!player.hasPermission("aevorinreports.bypass.limit")) {
            int maxActive = plugin.getConfigManager().getConfig().getReports().getMaxActiveReportsPerPlayer();
            long activeCount = plugin.getDatabaseManager().getReportsByReporter(player.getUniqueId()).stream()
                    .filter(r -> r.getStatus() == Report.ReportStatus.PENDING)
                    .count();

            if (activeCount >= maxActive) {
                MessageUtils.sendMessage(player, lang.getMessage("messages.report.limit-reached"));
                return true;
            }
        }

        // If reason is provided, create report directly
        if (args.length > 1) {
            String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            // Handle custom reason
            if (reason.equalsIgnoreCase("custom")) {
                if (!plugin.getConfig().getBoolean("reports.allow-custom-reasons", true)) {
                    MessageUtils.sendMessage(player, lang.getMessage("messages.error.custom-reason-disabled"));
                    return true;
                }

                plugin.getCustomReasonHandler().startCustomReason(player, targetPlayer);
                MessageUtils.sendMessage(player, lang.getMessage("messages.report.custom-reason-prompt"));
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
            LanguageManager lang = LanguageManager.get(plugin);
            List<String> suggestions = new ArrayList<>(lang.getReasonList());
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
        LanguageManager lang = LanguageManager.get(plugin);
        LocalDateTime now = LocalDateTime.now();
        Report report = Report.builder()
                .reporterUuid(reporter.getUniqueId())
                .reportedUuid(plugin.getServer().getOfflinePlayer(targetPlayer).getUniqueId())
                .reporterName(reporter.getName())
                .reportedPlayerName(targetPlayer)
                .reason(category)
                .serverName(plugin.getConfigManager().getConfig().getServerName())
                .status(Report.ReportStatus.PENDING)
                .coordinates(String.format("%.1f, %.1f, %.1f", reporter.getLocation().getX(),
                        reporter.getLocation().getY(), reporter.getLocation().getZ()))
                .world(reporter.getWorld().getName())
                .isAnonymous(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Save report to database
        DatabaseManager.getInstance().saveReport(report);

        // Send Discord notification
        if (plugin.getDiscordManager() != null) {
            plugin.getDiscordManager().sendReportNotification(report);
        }

        // Notify staff members
        String notification = lang.getMessage("messages.report.notification", Map.of(
            "reporter", reporter.getName(),
            "reported", targetPlayer,
            "category", category
        ));

        for (Player staff : plugin.getServer().getOnlinePlayers()) {
            if (staff.hasPermission("aevorinreports.notify")) {
                MessageUtils.sendMessage(staff, notification);
            }
        }

        // Notify reporter of success
        MessageUtils.sendMessage(reporter, lang.getMessage("messages.success.report-created"));

        // Update cooldown
        cooldowns.put(reporter.getUniqueId(), System.currentTimeMillis());
    }

    private String formatTime(long seconds) {
        if (seconds < 60)
            return seconds + "s";
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return minutes + "m " + remainingSeconds + "s";
    }

    private void showReportCategories(Player player, String targetPlayer) {
        String guiType = plugin.getConfig().getString("reports.gui.type", "book");

        if (guiType.equalsIgnoreCase("container")) {
            new ReportReasonContainerGUI(plugin).showReasonContainerGUI(player, targetPlayer);
        } else {
            showReportBookGUI(player, targetPlayer);
        }
    }

    private void showReportBookGUI(Player player, String targetPlayer) {
        new BookGUI(plugin).showReportCategories(player, targetPlayer);
    }
}